(ns benefactor.route
  (:require
    [domkm.silk :as silk]
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

(defn create
  "Given route data, create silk routes"
  [route-data]
  (silk/routes route-data))

(defn route->path
  "Given silk-routes, route-name and optional parameters and optional
  error-callback, creates a link to the route-name when possible. Create a link
  to empty hash otherwise.  E.g.:
  (route->path [[:sample [[\"sample\" (silk/int :id)]]]] :sample {:id 5})
  returns \"/sample/5\" The default error-callback is called with a map
  containing a key args with the route-name and params and a key error with the
  expection and prints the map and returns \"#!/\""
  ([silk-routes route-name]
   (route->path silk-routes route-name nil))
  ([silk-routes route-name params]
   (route->path silk-routes route-name params #(do
                                                 (console.error ::href-failed %)
                                                 (path->hash "/"))))
  ([silk-routes route-name params error-callback]
   (try
     (path->hash (silk/depart silk-routes route-name params))
     (catch js/Error e
       (error-callback {:args [route-name params]
                        :error e})))))

(defn navigate!
  "Given a router, silk-routes, route-name and optional params and optional
  error-callback, navigate to a route when possible. Navigate to empty hash
  otherwise, or if provided a callback navigate to the return value of the
  callback function. Check the route->path function docs about the callback
  arguments.  E.g.: `(go! app-route :sample {:id 6})`"
  ([router silk-routes route-name]
   (navigate! router silk-routes route-name nil))
  ([router silk-routes route-name params]
   (.setToken router (token->path (route->path silk-routes route-name params))))
  ([router silk-routes route-name params error-callback]
   (.setToken router (token->path (route->path silk-routes route-name params error-callback)))))

(defn hash-router
  "Given a callback to be called with the event on Navigate returns a History
  instance. You can then use (.setToken (hash-router callback)) to change the
  hash. Changing the hash will trigger the navigate-callback. Docs:
  https://google.github.io/closure-library/api/goog.history.Event.html"
  [navigate-callback]
  (doto (goog.History.)
    (goog.events/listen goog.History/EventType.NAVIGATE navigate-callback)
    (.setEnabled true)))

(defn path->route
  "Given a path \"/foo\" returns the matched silk-route or nil."
  [silk-routes path]
  (silk/arrive silk-routes path))

(defn navigate-event->token
  "Given a navigate event (goog.History/EventType.NAVIGATE) returns the hash
  token without the `#`. E.g.: if the route is
  \"http://exemple.com/#!/profile\" when the event is triggered it will return
  \"!/profile\""
  [event]
  (.-token event))

(defn setup-router
  "Given silk-routes, f and optional f-error  add a listener to update routes.
  When the route change, if the route matches f is called with a vector of
  [path parsed-route], else f-error is called on the current path, returns the
  router."
  ([silk-routes f]
   (setup-router silk-routes f (fn [path] (console.error ::route-not-found path))))
  ([silk-routes f f-error]
   (hash-router
     (fn [event]
       (let [path (-> event navigate-event->token token->path)]
         (if-let [parsed-route (path->route silk-routes path)]
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
