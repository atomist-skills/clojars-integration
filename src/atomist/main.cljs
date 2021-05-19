;; Copyright © 2021 Atomist, Inc.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns atomist.main
  (:require [atomist.api :as api]
            [cljs.core.async :refer [<!] :refer-macros [go]]
            [goog.string.format]
            [clojure.data]
            [atomist.github]))

(defn transact-config
  [handler]
  (fn [request]
    (go
      (when (= "config-change.edn" (-> request :subscription :name))
        (let [params (->> request :subscription :result (map first) (into {}))]
          (<! (api/transact request [{:schema/entity-type :maven/repository
                                      :maven.repository/url "https://repo.clojars.org"
                                      :maven.repository/username (get params "email")
                                      :maven.repository/secret (get params "deploy-token")
                                      :atomist.skill.capability/name "MavenRepository"
                                      :atomist.skill.capability/namespace "atomist"
                                      :maven.repository/repository-id "clojars"}]))))
      (<! (handler request)))))

(defn ^:export handler
  [data sendreponse]
  (api/make-request
   data
   sendreponse
   (-> (api/finished)
       (transact-config)
       (api/log-event)
       (api/status))))
