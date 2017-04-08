(ns halftone.app
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<!]]))

(def resolution 144)

(defn translate-dimension [dimension resolution]
  (->> resolution
       (* 25.4)
       (/ dimension)
       int))

(defn max-radius [raster-size resolution]
  (-> raster-size
      (/ 25.4)
      (* resolution)
      (/ 2)))

(defn raster-color [color-string]
  (vector (subs color-string 0 2)
          (subs color-string 2 4)
          (subs color-string 4 6)))

(defn square-size [max-radius]
  (* 2.0
     (/ (- max-radius 1.0)
        (.sqrt js/Math 2.0))))

(defn squares-per-paper [paper-dimension square-size]
  (/ paper-dimension square-size))

(defn pages-max [image-dimension paper-dimension]
  (.ceil js/Math 4))

(defn aspect [original-dimension display-image-dimension]
  (/ (* 1.0 original-dimension)
     (display-image-dimension)))

(defn utlization-area [dim1 dim2 aspect]
  (vector (* dim1 aspect)
          (* dim2 aspect)))

(defn pages [image-dim paper-dim]
  (->> paper-dim
       (/ (* 1.0 image-dim))
       int))

(defn squares-per-dim [paper-dim square-size]
  (int (/ paper-dim square-size)))

(defn rasterbate-page [x-page y-page square-size]
  (let [squares-x (squares-per-paper x-page square-size)
        squares-y (squares-per-paper y-page square-size)] (for [sy (range -1 (+ 1 squares-y))
                                                                sx (range -1 (+ 1 squares-x))]

                                                            [sy sx])))

(defn draw-circle [x y psy radius])
  ;; C.circle(ex, psy-ey, Radius/2)

(defn rasterbate! [pages-x pages-y square-size]
  (for [x-page pages-x
        y-page pages-y]
    (rasterbate-page x-page y-page square-size)))

(defn rows [width pixel-vector]
  (partition width pixel-vector))

(defn squares
  "transforms a pixel vector of:
  [1  2  3  4  5  6  7  8  9
   10 11 12 13 14 15 16 17 18
   19 20 21 22 23 24 25 26 27]
  into:
  [[1  2  3  [4  5  6   [7  8  9
   10 11 12   13 14 15   16 17 18
   19 20 21]  22 23 24]  25 26 27]]
  assuming width of 9, height of 3,
  square-width of 3, and square-height of 3.
  "
  [pixel-vector width height square-width square-height]
  (let [rs (rows width pixel-vector)
        row-groups (partition square-height
                              (map (partial partition square-width)
                                   rs))]

    (mapcat #(apply map concat %)
            row-groups)))

(defn pixel-brightness [{:keys [r g b]}]
  (+ (* 0.2126 r)
     (* 0.7152 g)
     (* 0.0722 b)))

(defn radius [brightness sample-size max-radius]
  (* (- 1
        (/ brightness sample-size))
     max-radius))

(defprotocol Semigroup
  (combine [this that]))

(defrecord Pixel [brightness r g b a]
  Semigroup
  (combine [this that]
    (-> this
        (update :brightness + (pixel-brightness that))
        (update :r + (:r that))
        (update :g + (:g that))
        (update :b + (:b that))
        (update :a + (:a that)))))

(defn average-pixel-group
  "for every pixel in square, sum the pixels
  by their channels, and then divide by the number
  of pixels in the square to find the average pixel
  for the square."
  [square]
  (let [number-of-pixels (count square)
        combined-pixel (reduce combine square)]

    (reduce (fn [pixel channel]
              (update pixel
                      channel
                      (fn [old]
                        (.floor js/Math
                                (/ old number-of-pixels)))))
            combined-pixel
            [:brightness :r :g :b :a])))

(defn get-element! [el-id]
  (.getElementById js/document el-id))

(defn create-canvas-element! []
  (.createElement js/document "canvas"))

(defn get-canvas-context! [canvas]
  (.getContext canvas "2d"))

(defn bytes->pixels
  "transform an array of bytes into pixels."
  [byte-array]
  (let [length (.-length byte-array)]
    (loop [data []
           i 0]
      (if (< i length)
        (recur (conj data (aget byte-array i))
               (inc i))
        (map (partial apply ->Pixel 0)
             (partition 4 data))))))

(defn get-image-data-from-element!
  "in order to get information about the image,
  it must be in a canvas element, so put the image into a canvas element
  and return its data"
  [image-element-name]
  (let [image  (get-element! image-element-name)
        canvas (create-canvas-element!)
        context (get-canvas-context! canvas)]

    ;; construct the canvas
    (set! (.-width canvas) (.-width image))
    (set! (.-height canvas) (.-height image))

    ;; draw the image into the canvas context
    (.drawImage context image 0 0)

    ;; and get the data
    (-> context
        (.getImageData 0                ;; x coordinate
                       0                ;; y coordinate
                       (.-width image)  ;; full width
                       (.-height image)) ;; full height
        .-data)))

(defn init []
  (let [image-data (get-image-data-from-element! "trmp")
        pixel-vector (bytes->pixels image-data)
        image (get-element! "trmp")
        width (.-width image)
        height (.-height image)
        sqrs (squares pixel-vector width height 20 20)]

    (.log js/console (clj->js (take 5 sqrs)))
    (.log js/console (clj->js (map average-pixel-group (take 5 sqrs))))
    (.log js/console "width:" width)
    (.log js/console "height:" height)))
    ;; (.log js/console (clj->js squares))))
