(ns winglue-well.pages.loadingpage)

(defn LoadingPage []
  [:div.center-on-page {:style {:font-size "3em"}}
   [:i.fa.fa-spinner.fa-pulse]
   " Initializing..."])

