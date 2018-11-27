(ns wmatson.clj-scryer.core
  (:import [javax.swing JFrame JLabel JButton BoxLayout JPanel]
           (java.awt.event ActionListener))
  (:require [orchard.inspect :as ori]))

(defonce current-inspector (atom (assoc (ori/fresh) :page-size 16)))
(defonce ^:private frame* (JFrame.))

#_(:rendered (swap! current-inspector ori/start (map #(hash-map :num %) (range))))

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

(defn- new-button [label action]
  (doto (JButton. label)
    (.addActionListener (reify ActionListener
                                 (actionPerformed [_ _] (action))))))

(defn render-inspector
  ([] (render-inspector @current-inspector))
  ([inspector]
   (.. frame*
       getContentPane
       removeAll)
   (.setLayout frame* (BoxLayout. (.getContentPane frame*) BoxLayout/Y_AXIS))
   (.add (.getContentPane frame*) (new-button "Pop Up" #(do (swap! current-inspector ori/up)
                                                            (render-inspector))))
   (loop [[next-value & remaining] (:rendered @current-inspector)
          current-container (new-panel frame*)]
     (when next-value
       (condp = (first next-value)
         :newline (recur remaining (new-panel frame*))
         :value (do (.add current-container (JLabel. (second next-value)))
                    (.add current-container (new-button "Drill Down" #(do (swap! current-inspector ori/down (last next-value))
                                                                          (render-inspector))))
                    (recur remaining current-container))
         (do
           (.add current-container (JLabel. next-value))
           (recur remaining current-container)))))
    (.pack frame*)
    (.setVisible frame* true)))

(defn inspect [value]
  (swap! current-inspector ori/start value)
  (render-inspector))