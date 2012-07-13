clj-irc
=======

clojure irc client based on pircbotx.

This project is currently at an early development stage.

```clojure
(ns clj-irc.example
  (:use [clj-irc.core]))

(defbot {:nick "test" :host "somehost" :server-password "server-password"
         :channels ["#test"]
         :auto-reconnect true}
        (on-message {:keys [content]} reply-to {:regexp #"^test:.*"}
          (reply-to (str "did you really say :" content))))
```