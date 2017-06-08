(ns winglue-well.widgets.loadingoverlay)

(defn LoadingOverlay []
  "Displays a loading spinner overlayed on top of a component.
   Put it in the same scope as the item to display the icon on top
   e.g. [:div [ItemToBeLoaded] [LoadingOverlay]]"
  [:div.loading-overlay
   [:i.fa.fa-refresh.fa-spin]])

