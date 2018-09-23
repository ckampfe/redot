(ns redot.app
  (:require [redot.drawing :as drawing]
            [re-frame.core :as rf]
            [reagent.core :as reagent]))

(rf/reg-event-db
 :initialize
 (fn [_ _]
   {:dot-size-slider {:value 30}}))

(rf/reg-event-db
 :dot-size-slider-change
 (fn [db [event-name value]]
   (assoc-in db
             [:dot-size-slider :value]
             value)))

(rf/reg-sub
 :dot-size
 (fn [db] (-> db :dot-size-slider :value)))

(defn hello []
  [:h1 "Hello, Redot"])

(defn dot-size-slider [min max]
  [:div
   (str "Dot size: " @(rf/subscribe [:dot-size]))
   [:br]
   [:input {:type "range"
            :value @(rf/subscribe [:dot-size])
            :min min
            :max max
            :on-change #(rf/dispatch [:dot-size-slider-change (-> % .-target .-value)])}]])

(defn ui []
  [:div
   [hello]
   [dot-size-slider 1 200]])

(defn ^:export -main []
  (rf/dispatch-sync [:initialize])
  (reagent/render [ui]
                  (.getElementById js/document "app")))

(-main)
