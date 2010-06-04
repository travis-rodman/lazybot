(ns sexpbot.plugins.login
  (:use [sexpbot respond info]
	[clj-config.core :only [read-config]])
  (:require [irclj.irclj :as ircb]))

(defn check-pass-login [user pass irc]
  (let [userconf (((:users (read-config info-file)) (:server @irc)) user)]
    (when (= pass (:pass userconf)) 
      (dosync (alter irc assoc-in [logged-in user] (userconf :privs))))))

(defn logged-in? [user] (some #{user} (keys @logged-in)))

(defplugin
  (:add-hook :on-quit
	    (fn [{:keys [nick]}] (when (logged-in? nick) (dosync (alter logged-in dissoc nick)))))

  (:login 
   "Best executed via PM. Give it your password, and it will log you in."
   ["login"] 
   [{:keys [irc nick channel args]}]
   (if (check-pass-login nick (first args) irc)
     (ircb/send-message irc channel "You've been logged in.")
     (ircb/send-message irc channel "Username and password combination do not match.")))
  
  (:logout 
   "Logs you out."
   ["logout"] 
   [{:keys [irc nick channel]}]
   (dosync (alter irc update-in [logged-in] dissoc nick)
	   (ircb/send-message irc channel "You've been logged out.")))

   (:privs
   "Finds your privs"
   ["privs"]
   [{:keys [irc channel nick]}]
   (do
     (ircb/send-message irc channel 
			(str nick ": You are a"
			     (if (not= :admin (:privs (((:users (read-config info-file)) (:server @irc)) nick)))
			       " regular user."
			       (str "n admin; you are " 
				    (if (logged-in nick) "logged in." "not logged in!"))))))))