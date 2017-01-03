(set-env!
  :dependencies '[[org.clojure/clojure "1.9.0-alpha14"]
                  [org.clojure/clojurescript "1.9.293"]

                  [com.cognitect/transit-cljs "0.8.239"]
                  [com.domkm/silk "0.1.2"]

                  [adzerk/boot-cljs "1.7.228-2" :scope "test"]
                  [crisptrutski/boot-cljs-test "0.3.0" :scope "test"]
                  [juxt/iota "0.2.3" :scope "test"]]
  :resource-paths #{"src"})


(require
  '[crisptrutski.boot-cljs-test :refer [test-cljs]])

(task-options!
 pom {:project 'benefactor
      :version "0.0.1-SNAPSHOT"
      :description "A library of clojurescript helpers"
      :url "https://mynomoto.github.io/benefactor"
      :scm {:url "https://github.com/mynomoto/benefactor"}
      :license {"Eclipse Public License"
                "http://www.eclipse.org/legal/epl-v10.html"}}
 push {:repo "deploy-clojars"})

(deftask build
  "Build and install the project locally."
  []
  (comp (pom) (jar) (install)))

(deftask testing []
  (merge-env! :source-paths #{"test"})
  identity)

(deftask autotest
  []
  (comp
    (testing)
    (watch)
    (speak)
    (test-cljs)))

(deftask run-tests
  []
  (comp
    (test-cljs :exit? true)
    (testing)))

(deftask local-install []
  (comp
    (pom)
    (jar)
    (install)))

(deftask release []
  (merge-env!
    :repositories [["deploy-clojars" {:url "https://clojars.org/repo"
                                      :username (System/getenv "CLOJARS_USER")
                                      :password (System/getenv "CLOJARS_PASS")}]])
  (comp
    (local-install)
    (sift :include [#".*\.jar"])
    (push)))
