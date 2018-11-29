(ns wmatson.clj-scryer.core
  (:import [javax.swing JFrame JLabel JButton BoxLayout JPanel]
           (java.awt.event ActionListener))
  (:require [orchard.inspect :as ori]))

(defonce current-inspector (atom (assoc (ori/fresh) :page-size 16)))
(defonce ^:private frame-singleton (JFrame.))

;;Lifted from orchard.inspect/inspect-print and modified
(defn print-inspector
  ([] (print-inspector @current-inspector))
  ([inspector]
   (print
     (with-out-str
       (doseq [component (:rendered inspector)]
         (ori/inspect-print-component component))))))

(defn- new-panel [frame]
  (let [panel (JPanel.)]
    (.add (.getContentPane frame) panel)
    panel))

(defn- new-button [^String label action]
  (doto (JButton. label)
    (.addActionListener (reify ActionListener
                                 (actionPerformed [_ _] (action))))))

(defn render-inspector []
  (.. frame-singleton
      getContentPane
      removeAll)
  (.setLayout frame-singleton (BoxLayout. (.getContentPane frame-singleton) BoxLayout/Y_AXIS))
  (doto (new-panel frame-singleton)
    (.add (new-button "Prev Page" #(swap! current-inspector ori/prev-page)))
    (.add (new-button "Pop Up" #(swap! current-inspector ori/up)))
    (.add (new-button "Next Page" #(swap! current-inspector ori/next-page))))
  (loop [[next-value & remaining] (:rendered @current-inspector)
         current-container (new-panel frame-singleton)]
    (when next-value
      (condp = (first next-value)
        :newline (recur remaining (new-panel frame-singleton))
        :value (do (.add current-container (JLabel. ^String (second next-value)))
                   (.add current-container (new-button "Drill Down" #(swap! current-inspector ori/down (last next-value))))
                   (recur remaining current-container))
        (do
          (.add current-container (JLabel. ^String next-value))
          (recur remaining current-container)))))
  (.pack frame-singleton)
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