package no.boostai.sdk.UI.Events

import java.lang.ref.WeakReference

object BoostUIEvents {
    private var observers = ArrayList<WeakReference<Observer>>()

    fun addObserver(observer: Observer) {
        observers.add(WeakReference(observer))
    }

    fun removeObserver(observer: Observer) {
        var remove: WeakReference<Observer>? = null
        observers.forEach {
            if (it.get() == observer) {
                remove = it
            }
        }

        if (remove != null) {
            observers.remove(remove)
        }
    }

    fun notifyObservers(event: Event, detail: Any? = null) {
        observers.forEach { observer ->
            observer.get()?.onUIEventReceived(event, detail)
        }
    }
    
    interface Observer {
        fun onUIEventReceived(event: Event, detail: Any?)
    }

    enum class Event {
        chatPanelOpened,
        chatPanelClosed,
        chatPanelMinimized,
        conversationIdChanged,
        messageSent,
        menuOpened,
        menuClosed,
        privacyPolicyOpened,
        conversationDownloaded,
        conversationDeleted,
        positiveMessageFeedbackGiven,
        negativeMessageFeedbackGiven,
        positiveConversationFeedbackGiven,
        negativeConversationFeedbackGiven,
        conversationFeedbackTextGiven,
        actionLinkClicked,
        externalLinkClicked,
        conversationReferenceChanged,
        filterValuesChanged,
    }
}