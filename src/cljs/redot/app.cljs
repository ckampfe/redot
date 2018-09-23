(ns redot.app
  (:require [redot.drawing :as drawing]
            [re-frame.core :as rf]
            [reagent.core :as reagent]))

(rf/reg-event-db
 :initialize
 (fn [_ _]
   {:dot-size-slider {:value 30}
    :draw-sigil (cljs.core/random-uuid)
    :square {:width 10
             :height 10}}))

(rf/reg-event-db
 :dot-size-slider-change
 (fn [db [event-name value]]
   (assoc-in db
             [:dot-size-slider :value]
             value)))

(rf/reg-event-db
 :square-height-slider-change
 (fn [db [event-name value]]
   (assoc-in db
             [:square :height]
             value)))

(rf/reg-event-db
 :square-width-slider-change
 (fn [db [event-name value]]
   (assoc-in db
             [:square :width]
             value)))

(rf/reg-event-db
 :redraw
 (fn [db [event-name value]]
   (drawing/redraw)
   (assoc db :draw-sigil (cljs.core/random-uuid))
   ))

(rf/reg-sub
 :dot-size
 (fn [db] (-> db :dot-size-slider :value)))

(rf/reg-sub
 :draw-sigil
 (fn [db]
   (-> db :draw-sigil)))


(rf/reg-sub
 :square-width
 (fn [db]
   (-> db :square :width)))

(rf/reg-sub
 :square-height
 (fn [db]
   (-> db :square :height)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defn square-width-slider [min max]
  [:div
   (str "Square width: " @(rf/subscribe [:square-width]))
   [:br]
   [:input {:type "range"
            :value @(rf/subscribe [:square-width])
            :min min
            :max max
            :on-change #(rf/dispatch [:square-width-slider-change (-> % .-target .-value)])}]])

(defn square-height-slider [min max]
  [:div
   (str "Square height: " @(rf/subscribe [:square-height]))
   [:br]
   [:input {:type "range"
            :value @(rf/subscribe [:square-height])
            :min min
            :max max
            :on-change #(rf/dispatch [:square-height-slider-change (-> % .-target .-value)])}]])

(defn draw-container []
  [:div
   [:canvas {:id "redot"}]
   [:img {:id "input-image"
          :src "tuck.png"}]])

(defn redraw-button []
  [:input {:type "button"
           :value "Redraw"
           :on-click #(rf/dispatch [:redraw])}])

(defn ui []
  [:div
   [hello]
   [redraw-button]
   [dot-size-slider 1 200]
   [square-width-slider 1 200]
   [square-height-slider 1 200]
   [draw-container]])

(defn ^:export -main []
  (rf/dispatch-sync [:initialize])
  (reagent/render [ui]
                  (.getElementById js/document "app"))
  (redot.drawing/do-sketch))

(-main)
