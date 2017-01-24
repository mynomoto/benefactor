(ns benefactor.route-test
  (:require
    [benefactor.route]
    [clojure.test :refer-macros [deftest testing is async]]
    [juxt.iota :refer [given]]))

(def routes
  ["" [["" :index]
       ["/"
        [["" :index]
         ["profile" :profile]
         [["profile/" "edit"] :profile-edit]
         [["exploration/" :url-id] :exploration]
         [true :not-found]]]]])

(deftest setup-and-change-with-reset
  (let [current-path (atom nil)
        current-matched-route (atom nil)
        router (benefactor.route/setup-router
                 routes
                 (fn [[path matched-route]]
                   (reset! current-path path)
                   (reset! current-matched-route matched-route)))
        _ (benefactor.route/sync-from-atom router current-path)]
    (is (= "" @current-path))
    (is (= :index (:handler @current-matched-route)))

    (reset! current-path "/profile")
    (is (= "#!/profile" (-> js/window .-location .-hash)))
    (is (= "!/profile" (.getToken router)))
    (is (= "/profile" @current-path))
    (async done
      (js/setTimeout
        #(do
           (is (= "/profile" @current-path))
           (is (= :profile (:handler @current-matched-route)))
           (done))
        0))))

(deftest change-with-hash-set
  (let [current-path (atom nil)
        current-matched-route (atom nil)
        router (benefactor.route/setup-router
                 routes
                 (fn [[path matched-route]]
                   (when (not= path @current-path)
                     (reset! current-path path)
                     (reset! current-matched-route matched-route))))
        _ (benefactor.route/sync-from-atom router current-path)]
    (-> js/window .-location .-hash  (set! "#!/profile/edit"))
    (is (= "#!/profile/edit" (-> js/window .-location .-hash)))
    (is (= "!/profile/edit" (.getToken router)))
    (async done
      (js/setTimeout
        #(do
           (is (= "/profile/edit" @current-path))
           (is (= :profile-edit (:handler @current-matched-route)))
           (done))
        0))))

(deftest hash->path
  (is (= "/zzz" (benefactor.route/hash->path "#!/zzz")))
  (is (= "/asd/aaa" (benefactor.route/hash->path "#!/asd/aaa"))))

(deftest token->path
  (is (= "/zzz" (benefactor.route/token->path "!/zzz")))
  (is (= "/asd/aaa" (benefactor.route/token->path "!/asd/aaa"))))

(deftest path->hash
  (is (= "#!/zzz" (benefactor.route/path->hash "/zzz")))
  (is (= "#!/asd/aaa" (benefactor.route/path->hash "/asd/aaa"))))

(deftest path->token
  (is (= "!/zzz" (benefactor.route/path->token "/zzz")))
  (is (= "!/asd/aaa" (benefactor.route/path->token "/asd/aaa"))))

(deftest path-for
  (is (= "" (benefactor.route/path-for routes :index)))
  (is (= "/profile" (benefactor.route/path-for routes :profile)))
  (is (= "/profile?aaa=bbb" (benefactor.route/path-for routes :profile {:query-params {:aaa "bbb"}})))
  (is (= "/exploration/amazing?aaa=bbb" (benefactor.route/path-for routes :exploration {:url-id "amazing" :query-params {:aaa "bbb"}}))))

(deftest route->path
  (is (= "" (benefactor.route/route->path routes :index))))
