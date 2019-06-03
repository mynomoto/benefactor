(set-env!
  :dependencies '[[org.clojure/clojure "1.10.0"]
                  [org.clojure/clojurescript "1.10.520"]

                  [com.cognitect/transit-cljs "0.8.256"]
                  [bidi "2.1.6"]

                  [adzerk/boot-cljs "1.7.228-2" :scope "test"]
                  [crisptrutski/boot-cljs-test "0.3.0" :scope "test"]
                  [juxt/iota "0.2.3" :scope "test"]]
  :resource-paths #{"src"})

(ns-unmap *ns* 'test)

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

(deftask test
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
