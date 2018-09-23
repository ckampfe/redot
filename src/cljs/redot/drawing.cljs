(ns redot.drawing
  (:require [quil.core :as q :include-macros true]))

(def host "redot")

(defn get-element! [el-id]
  (.getElementById js/document el-id))

(defn create-canvas-element! []
  (.createElement js/document "canvas"))

(defn get-canvas-context! [canvas]
  (.getContext canvas "2d"))

(defn radius [brightness sample-size max-radius]
  (* (- 1
        (/ brightness sample-size))
     max-radius))

(defn into-squares
  "assuming width of 9, height of 6,
   square-width of 3, and square-height of 3,
   the progression of data looks like this:
   initial:
   (1  2  3  4  5  6  7  8  9
    10 11 12 13 14 15 16 17 18
    19 20 21 22 23 24 25 26 27
    28 29 30 31 32 33 34 35 36
    37 38 39 40 41 42 43 44 45
    46 47 48 49 50 51 52 53 54)
   with rows:
   ((1  2  3  4  5  6  7  8  9)
    (10 11 12 13 14 15 16 17 18)
    (19 20 21 22 23 24 25 26 27)
    (28 29 30 31 32 33 34 35 36)
    (37 38 39 40 41 42 43 44 45)
    (46 47 48 49 50 51 52 53 54))
   rows partitioned with square-width:
   (((1  2  3)  (4  5  6)  (7  8  9))
    ((10 11 12) (13 14 15) (16 17 18))
    ((19 20 21) (22 23 24) (25 26 27))
    ((28 29 30) (31 32 33) (34 35 36))
    ((37 38 39) (40 41 42) (43 44 45))
    ((46 47 48) (49 50 51) (52 53 54)))
   rows grouped/partitioned with square-height:
   (note that rows beginning with (1 2 3), (10 11 12),
   and (19 20 21) appear in the same collection,
   reflecting the square-height of 3)
   ((((1  2  3)  (4  5  6)  (7  8  9))
     ((10 11 12) (13 14 15) (16 17 18))
     ((19 20 21) (22 23 24) (25 26 27)))
    (((28 29 30) (31 32 33) (34 35 36))
     ((37 38 39) (40 41 42) (43 44 45))
     ((46 47 48) (49 50 51) (52 53 54))))
   squares, where each row has been pivoted
   such that the 1st item of row 1
   is concatenated with the 1st item of rows
   2 and 3, and the second item of row one with
   the second items of rows 2 and 3, etc.
   ((1 2 3 10 11 12 19 20 21)
    (4 5 6 13 14 15 22 23 24)
    (7 8 9 16 17 18 25 26 27)
    (28 29 30 37 38 39 46 47 48)
    (31 32 33 40 41 42 49 50 51)
    (34 35 36 43 44 45 52 53 54))"
  [pixel-vector width height square-width square-height]
  (let [row-groups (->> pixel-vector
                        (partition width)
                        (map (partial partition square-width))
                        (partition square-height))]

    ;; `apply` is used here
    ;; to make use of the variadic
    ;; nature of `map`
    (mapcat #(apply map concat %)
            row-groups)))

(defn pixel-brightness
  "this is based on something I found on the internet,
  unfortunately I've lost the link"
  [{:keys [r g b]}]
  (+ (* 0.2126 r)
     (* 0.7152 g)
     (* 0.0722 b)))

(defprotocol Semigroup
  "a typeclass for things that can be combined"
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

;; function printCanvas()  
;; {  
;;  var dataUrl = document.getElementById('anycanvas').toDataURL(); //attempt to save base64 string to server using this var  
;;  var windowContent = '<!DOCTYPE html>';
;;  windowContent += '<html>'
;;  windowContent += '<head><title>Print canvas</title></head>';
;;  windowContent += '<body>'
;;  windowContent += '<img src="' + dataUrl + '">';
;;  windowContent += '</body>';
;;  windowContent += '</html>';
;;  var printWin = window.open('','','width=340,height=260');
;;  printWin.document.open();
;;  printWin.document.write(windowContent);
;;  printWin.document.close();
;;  printWin.focus();
;;  printWin.print();
;;  printWin.close();
;;  }

#_(defn print-canvas []
    (let [data-url (.toDataURL (get-element! "redot"))]))

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
        ;; canvas (create-canvas-element!)
        ;; context (get-canvas-context! canvas)
        canvas (get-element! "redot")
        context (get-canvas-context! canvas)]

    ;; construct the canvas
    (set! (.-width canvas) (.-width image))
    (set! (.-height canvas) (.-height image))

    ;; draw the image into the canvas context
    (.drawImage context image 0 0)

    ;; and get the data
    (-> context
        (.getImageData 0                 ;; x coordinate
                       0                 ;; y coordinate
                       (.-width image)   ;; full width
                       (.-height image)) ;; full height
        .-data)))

(defn load-app-db []
  @re-frame.db/app-db)

(defn draw []
  (let [db (load-app-db)
        image-data (get-image-data-from-element! "input-image")
        pixel-vector (bytes->pixels image-data)
        image (get-element! "input-image")
        width (.-width image)
        height (.-height image)
        square-width (int (-> db :square :width))
        square-height (int (-> db :square :height))
        squares (into-squares pixel-vector width height square-width square-height)
        square-averages (map average-pixel-group squares)
        square-centers (square-center-coordinates width height square-width square-height)
        pixels-with-locations (partition 2 (interleave square-averages square-centers))]

    (q/background 255)
    (.log js/console "Cleared canvas")

    (doseq [[pixel [x y]] pixels-with-locations]
      (q/with-fill [(:r pixel)
                    (:g pixel)
                    (:b pixel)
                    (:a pixel)]
        ;; this radius value has an incredible effect
        ;; on the quality of the resulting drawing
        ;; TODO: look into have to better the radius
        (let [r (/ (radius (:brightness pixel) 400 (-> db :dot-size-slider :value))
                   2)]
          (q/ellipse x y r r))))))


(defn setup []
  (q/background 255)
  (q/no-stroke)
  (q/no-loop))

(defn redraw []
  (q/with-sketch (q/get-sketch-by-id host)
    (q/redraw)
    (.log js/console "Redrew canvas")))

(defn do-sketch []
  (q/defsketch redot
    :title "redot"
    :host host
    :size [250 250]
    :setup setup
    :draw draw))
