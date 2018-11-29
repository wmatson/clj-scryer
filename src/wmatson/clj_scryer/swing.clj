(ns wmatson.clj-scryer.swing
  (:import [javax.swing JFrame JLabel JButton BoxLayout JPanel UIManager JScrollPane]
           (java.awt.event ActionListener)
           (java.awt BorderLayout Toolkit Dimension))
  (:require [orchard.inspect :as ori]))

(defonce current-inspector (atom (assoc (ori/fresh) :page-size 16)))
(defonce ^:private frame-singleton (do (UIManager/setLookAndFeel (UIManager/getSystemLookAndFeelClassName))
                                       (let [screen-size (.getScreenSize (Toolkit/getDefaultToolkit))
                                             starting-width (/ (.getWidth screen-size) 2)
                                             starting-height (* 3 (/ (.getWidth screen-size) 4))]
                                         (doto (JFrame. "clj-scryer")
                                           (.setSize (Dimension. starting-width starting-height))))))

;;Lifted from orchard.inspect/inspect-print and modified
(defn print-inspector
  ([] (print-inspector @current-inspector))
  ([inspector]
   (print
     (with-out-str
       (doseq [component (:rendered inspector)]
         (ori/inspect-print-component component))))))

(defn- add-panel [container]
  (let [panel (JPanel.)]
    (.add container panel)
    panel))

(defn- new-button [^String label action]
  (doto (JButton. label)
    (.addActionListener (reify ActionListener
                                 (actionPerformed [_ _] (action))))))

(defn render-inspector []
  (-> frame-singleton
      .getContentPane
      .removeAll)
  (.setLayout frame-singleton (BorderLayout.))
  (let [main-panel (JPanel.)
        top-bar (JPanel.)]
    (.setLayout main-panel (BoxLayout. main-panel BoxLayout/Y_AXIS))
    (doto top-bar
      (.add ^JPanel (new-button "Prev Page" #(swap! current-inspector ori/prev-page)))
      (.add ^JPanel (new-button "Pop Up" #(swap! current-inspector ori/up)))
      (.add ^JPanel (new-button "Next Page" #(swap! current-inspector ori/next-page))))
    (loop [[next-value & remaining] (:rendered @current-inspector)
           current-container (add-panel main-panel)]
      (when next-value
        (condp = (first next-value)
          :newline (recur remaining (add-panel main-panel))
          :value (do (.add current-container (JLabel. ^String (second next-value)))
                     (.add current-container (new-button ">" #(swap! current-inspector ori/down (last next-value))))
                     (recur remaining current-container))
          (do
            (.add current-container (JLabel. ^String next-value))
            (recur remaining current-container)))))
    (doto (.getContentPane frame-singleton)
      (.add top-bar BorderLayout/NORTH)
      (.add (doto (JScrollPane. main-panel)
              (-> .getVerticalScrollBar (.setUnitIncrement 16)))
            BorderLayout/CENTER)))
  (.setVisible frame-singleton true))

(add-watch current-inspector :rerender (fn [& _] (render-inspector)))

(defn inspect [value]
  ;;Prevent lazy-seqs from being fully-evaluated if called from repl
  (do (swap! current-inspector ori/start value)
      nil))

(comment
  (inspect (map first (iterate (fn [[a b :as fib]]
                                 (cons (+ a b) fib))
                               [1 1]))))