(ns winglue-well.oauth
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clj-oauth2.client :as oauth2]
            [cheshire.core :refer [parse-string]]
            [clj-http.client :as http]
            [clojure.string :as string]
            [ring.util.response :as response]))

(def config (edn/read-string (slurp (io/resource "authenconfig.edn"))))

(def oauth-params (merge {;; These should be set in the oauth-params.edn file
                          :redirect-uri "http://localhost:8000/oauth2callback"
                          :client-id "*google-client-id*"
                          :client-secret "*google-client-secret*"
                          :scope ["https://www.googleapis.com/auth/userinfo.email"]
                          ;; These don't need changes
                          :authorization-uri "https://accounts.google.com/o/oauth2/auth"
                          :access-token-uri "https://accounts.google.com/o/oauth2/token"
                          :access-query-param :access_token
                          :grant-type "authorization_code"
                          :access-type "online"
                          :approval_prompt ""
                          :hd "appsmiths.net"}
                         (:oauth-params config)))

(def auth-request (oauth2/make-auth-request oauth-params))

(defn verifed-appsmiths [req]
  (let [token (oauth2/get-access-token oauth-params
                                       (:params req)
                                       auth-request)
        token-info (:body (http/get "https://www.googleapis.com/oauth2/v1/tokeninfo"
                                    {:query-params {:access_token (:access-token token)}
                                     :as :json}))]
    ;(println (:email token-info))
    ;(if (string/ends-with? (google-user-email (:access-token token)) "@appsmiths.net")
    (if (string/ends-with? (:email token-info) "@appsmiths.net")
      (response/redirect (:success-url config "/"))
      (response/redirect (:notfound-url config "/")))))








