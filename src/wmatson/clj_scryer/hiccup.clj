(ns wmatson.clj-scryer.hiccup
  (:require [hiccup.core :as hccp]
            [orchard.inspect :as ori]
            [ring.server.standalone :as server]))

(defonce ^:private current-inspector (atom (ori/fresh)))
(defonce ^:private server (atom nil))

(defn- concat-line-strings [line]
  (->> line
       (partition-by string?)
       (map-indexed #(if (even? %1)
                       (apply str %2)
                       (apply concat %2)))
       (remove #(and (string? %) (clojure.string/blank? %)))))

(defn- command-button [command text & [param]]
  [:a.button {:href (str "/" command "/" param)} text])

(defn- iline->hiccup [line]
  (->> line
       concat-line-strings
       (reduce (fn [acc [k display idx :as next]]
                 (conj acc (if (#{:value} k)
                             [:span
                              [:span.is-family-monospace display]
                              (command-button "down" "&gt;" idx)]
                             [:span.tag next])))
               [:div])))

(defn- irender->hiccup [inspector-rendered]
  (conj [:div.container
         [:div.level
          (command-button "prev" "Prev")
          (command-button "up" "Up")
          (command-button "next" "Next")]]
        (->> inspector-rendered
             (partition-by (comp #{:newline} first))
             (partition-all 1 2)
             (apply concat)
             (map iline->hiccup))))

(defn- surround-in-page [hiccup]
  [:html
   [:head
    [:link {:rel "stylesheet" :href "https://cdnjs.cloudflare.com/ajax/libs/bulma/0.7.2/css/bulma.min.css"}]]
   [:body hiccup]])

(defn render-page [inspector]
  (-> inspector :rendered irender->hiccup surround-in-page hccp/html))

(defn handler [{:keys [uri]}]
  (when-let [[_ command param] (re-find #"^/([^/]+)/(.+)?$" uri)]
    (condp = command
      "down" (swap! current-inspector ori/down (Integer/parseInt param))
      "up" (swap! current-inspector ori/up)
      "prev" (swap! current-inspector ori/prev-page)
      "next" (swap! current-inspector ori/next-page)
      :ignore-otherwise))
  {:status 200 :body (render-page @current-inspector)})

(defn inspect [value]
  (swap! current-inspector ori/start value)
  (when-not @server
    (reset! server (server/serve handler {:port (Integer/getInteger "wmatson.scryer.hiccup.port" 31415)
                                          :join? false
                                          :open-browser? false})))
  (let [port (-> @server .getConnectors (aget 0) .getLocalPort)
        url (str "http://localhost:" port "/")]
    (clojure.java.browse/browse-url url)))

(comment
  (do (swap! server #(.stop %))
      (inspect (map #(hash-map :num %) (range)))))