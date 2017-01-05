(ns benefactor.route-test
  (:require
    [benefactor.route]
    [clojure.test :refer-macros [deftest testing is async]]
    [juxt.iota :refer [given]]))

(def routes
  (benefactor.route/create
    [[:index [[]]]
     [:profile [["profile"]]]
     [:profile-edit [["profile" "edit"]]]
     [:exploration [["exploration" :url-id]]]]))

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
    (is (= :index (:domkm.silk/name @current-matched-route)))

    (reset! current-path "/profile")
    (is (= "#!/profile" (-> js/window .-location .-hash)))
    (is (= "!/profile" (.getToken router)))
    (is (= "/profile" @current-path))
    (async done
      (js/setTimeout
        #(do
           (is (= "/profile" @current-path))
           (is (= :profile (:domkm.silk/name @current-matched-route)))
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
           (is (= :profile-edit (:domkm.silk/name @current-matched-route)))
           (done))
        0))))
