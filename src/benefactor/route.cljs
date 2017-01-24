(ns benefactor.route
  (:require
    [bidi.bidi :as bidi]
    [clojure.string :as str]
    [goog.History]
    [goog.events])
  (:import
    [goog.History]))

(defn hash->path
  "Given a hash in the format \"#!/xxx\" returns the path \"/xxx\""
  [token]
  (.substr token 2))

(defn token->path
  "Given a token in the format \"!/xxx\" returns the path \"/xxx\""
  [token]
  (.substr token 1))

(defn path->hash
  "Given a path \"/xxx\" retuns a hash to be used in links \"#!/xxx\""
  [path]
  (str "#!" path))

(defn path->token
  "Given a path \"/xxx\" retuns a token to be used by .setToken \"!/xxx\""
  [path]
  (str "!" path))

(defn url-encode
  [string]
  (-> (js/encodeURIComponent string)
      (.replace "+" "%20")))

(defn format-query-string
  "Given a map of params, returns the query string to be appended on the path."
  [params]
  (str "?"
    (->> params
         sort
         (map (fn [[k v]]
                (str (url-encode (name k)) "=" (url-encode (str v)))))
         (str/join "&"))))

(defn path-for
  "Given a route definition data structure, a handler and an params map, return
  a path that would route to the handler. The map must contain the values to
  any parameters required to create the path, and extra values are silently
  ignored. It's like bidi/path-for but with an map as params argument instead
  of key value pairs. It also adds a query-string when there is a :query-params
  key inside params."
  ([route handler] (path-for route handler nil))
  ([route handler params]
   (when (nil? handler)
     (throw (ex-info "Cannot form URI from a nil handler" {})))
   (if-let [query-params (not-empty (:query-params params))]
     (str (bidi/unmatch-pair route {:handler handler :params params}) (format-query-string query-params))
     (bidi/unmatch-pair route {:handler handler :params params}))))

(defn route->path
  "Given bidi-routes, route-name and optional parameters and optional
  error-callback, creates a link to the route-name when possible. Create a link
  to empty hash otherwise.  E.g.:
  (route->path [[[\"/sample/\" [long :id]]] :sample] :sample {:id 5})
  returns \"/sample/5\" The default error-callback is called with a map
  containing a key args with the route-name and params and a key error with the
  expection and prints the map and returns \"#!/\""
  ([bidi-routes route-name]
   (route->path bidi-routes route-name nil))
  ([bidi-routes route-name params]
   (route->path bidi-routes route-name params #(do
                                                 (console.error ::href-failed %)
                                                 (path->hash "/"))))
  ([bidi-routes route-name params error-callback]
   (try
     (path-for bidi-routes route-name params)
     (catch js/Error e
       (error-callback {:args [route-name params]
                        :error e})))))

(defn navigate!
  "Given a router, bidi-routes, route-name and optional params and optional
  error-callback, navigate to a route when possible. Navigate to empty hash
  otherwise, or if provided a callback navigate to the return value of the
  callback function. Check the route->path function docs about the callback
  arguments.  E.g.: `(go! app-route :sample {:id 6})`"
  ([router bidi-routes route-name]
   (navigate! router bidi-routes route-name nil))
  ([router bidi-routes route-name params]
   (.setToken router (token->path (path->hash (route->path bidi-routes route-name params)))))
  ([router bidi-routes route-name params error-callback]
   (.setToken router (token->path (path->hash (route->path bidi-routes route-name params error-callback))))))

(defn hash-router
  "Given a callback to be called with the event on Navigate returns a History
  instance. You can then use (.setToken (hash-router callback)) to change the
  hash. Changing the hash will trigger the navigate-callback. Docs:
  https://google.github.io/closure-library/api/goog.history.Event.html"
  [navigate-callback]
  (doto (goog.History.)
    (goog.events/listen goog.History/EventType.NAVIGATE navigate-callback)
    (.setEnabled true)))

(defn url-decode
  [string]
  (js/decodeURIComponent string))

(defn split-parameters
  [param]
  (let [[k v] (str/split param #"=")]
    [k (or v "")]))

(defn parse-query-string
  "Given a query string returns a map of params."
  [query-string]
  (when (not (str/blank? query-string))
    (->> query-string
         url-decode
         (str/split query-string #"&")
         (map split-parameters)
         (into {}))))

(defn get-query-string
  "Given a path, returns the query string when one is present."
  [route]
  (when (str/index-of route "?")
    (last (str/split route #"\?" 2))))

(defn path->route
  "Given a path \"/foo\" returns the matched route or nil."
  [bidi-routes path]
  (if-let [query-params (-> path get-query-string parse-query-string)]
    (assoc (bidi/match-route bidi-routes path)
           :query-params query-params)
    (bidi/match-route bidi-routes path)))

(defn navigate-event->token
  "Given a navigate event (goog.History/EventType.NAVIGATE) returns the hash
  token without the `#`. E.g.: if the route is
  \"http://exemple.com/#!/profile\" when the event is triggered it will return
  \"!/profile\""
  [event]
  (.-token event))

(defn setup-router
  "Given bidi-routes, f and optional f-error  add a listener to update routes.
  When the route change, if the route matches f is called with a vector of
  [path parsed-route], else f-error is called on the current path, returns the
  router."
  ([bidi-routes f]
   (setup-router bidi-routes f (fn [path] (console.error ::route-not-found path))))
  ([bidi-routes f f-error]
   (hash-router
     (fn [event]
       (let [path (-> event navigate-event->token token->path)]
         (if-let [parsed-route (path->route bidi-routes path)]
           (f [path parsed-route])
           (f-error path)))))))

(defn sync-from-atom
  "Given a router and a atom containing the current path setups a watch that
  will update the hash when the atom content changes to a different path."
  [router path-atom]
  (add-watch path-atom ::sync-router
    (fn [k r o n]
      (when (not= o n)
        (.setToken router (path->token n))))))

