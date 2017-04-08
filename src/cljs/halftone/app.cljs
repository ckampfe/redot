(ns halftone.app
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<!]]))

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

(defn square-center-coordinates
  "given width 10, height 10, square-width 4, square-height 4, returns:
  ([2 2] [6 2]
   [2 6] [6 6])
  "
  [width height square-width square-height]
  (for [heights (range (/ square-height 2.0)
                       height
                       square-height)
        widths (range (/ square-width 2.0)
                      width
                      square-width)]
    [widths heights]))

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
        square-width 20
        square-height 20
        sqrs (squares pixel-vector width height square-width square-height)
        square-averages (map average-pixel-group (take 5 sqrs))
        square-centers (square-center-coordinates width height square-width square-height)
        with-locations (interleave square-averages (take 5 square-centers))
        ]

    (.log js/console (clj->js with-locations))
    (.log js/console (clj->js (take 5 sqrs)))
    (.log js/console (clj->js (map average-pixel-group (take 5 sqrs))))
    (.log js/console "width:" width)
    (.log js/console "height:" height)))
    ;; (.log js/console (clj->js squares))))
