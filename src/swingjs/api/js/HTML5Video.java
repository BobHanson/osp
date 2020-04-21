package swingjs.api.js;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Function;

import swingjs.api.js.HTML5Video.Promise;

/**
 * A full-service interface for HTML5 video element interaction. Allows setting
 * and getting HTML5 video element properties. ActionListeners can be set to
 * listen for JavaScript events associated with a video element.
 * 
 * See https://developer.mozilla.org/en-US/docs/Web/API/HTMLMediaElement for
 * details.
 * 
 * @author hansonr
 *
 */
public interface HTML5Video {

	public interface Promise {

	}

	final static String[] eventTypes = new String[] { "audioprocess", // The input buffer of a ScriptProcessorNode is
																		// ready to be processed.
			"canplay", // The browser can play the media, but estimates that not enough data has been
						// loaded to play the media up to its end without having to stop for further
						// buffering of content.
			"canplaythrough", // The browser estimates it can play the media up to its end without stopping
								// for content buffering.
			"complete", // The rendering of an OfflineAudioContext is terminated.
			"durationchange", // The duration attribute has been updated.
			"emptied", // The media has become empty; for example, this event is sent if the media has
						// already been loaded (or partially loaded), and the load() method is called to
						// reload it.
			"ended", // Playback has stopped because the end of the media was reached.
			"loadeddata", // The first frame of the media has finished loading.
			"loadedmetadata", // The metadata has been loaded.
			"pause", // Playback has been paused.
			"play", // Playback has begun.
			"playing", // Playback is ready to start after having been paused or delayed due to lack of
						// data.
			"progress", // Fired periodically as the browser loads a resource.
			"ratechange", // The playback rate has changed.
			"seeked", // A seek operation completed.
			"seeking", // A seek operation began.
			"stalled", // The user agent is trying to fetch media data, but data is unexpectedly not
						// forthcoming.
			"suspend", // Media data loading has been suspended.
			"timeupdate", // The time indicated by the currentTimeattribute has been updated.
			"volumechange", // The volume has changed.
			"waiting", // Playback has stopped because of a temporary lack of data
	};

	// direct methods
	
	public void addTextTrack() throws Throwable;
	public Object captureStream() throws Throwable;
	public String canPlayType(String mediaType) throws Throwable;
	public void fastSeek(double time) throws Throwable;
	public void load() throws Throwable;
	public void mozCaptureStream() throws Throwable;
	public void mozCaptureStreamUntilEnded() throws Throwable;
	public void mozGetMetadata() throws Throwable;
	public void pause() throws Throwable;
	public Promise play() throws Throwable;
	public Promise seekToNextFrame() throws Throwable;
	public Promise setMediaKeys(Object mediaKeys) throws Throwable;
	public Promise setSinkId(String id) throws Throwable;

	// convenience methods
	
	public static double getDuration(HTML5Video v) {
		return /** @j2sNative v.duration || */
		0;
	}

	public static double setCurrentTime(HTML5Video v, double time) {
		return /** @j2sNative v.currentTime = time|| */
		0;
	}

	public static double getCurrentTime(HTML5Video v) {
		return /** @j2sNative v.currentTime|| */
		0;
	}

	// property setting and getting
	
	/**
	 * Set a property of the the HTML5 video element using jsvideo[key] = value. 
	 * Numbers and Booleans will be unboxed.
	 * 
	 * @param jsvideo the HTML5 video element
	 * @param key
	 * @param value
	 */
	public static void setProperty(HTML5Video jsvideo, String key, Object value) {
		if (value instanceof Number) {
			/** @j2sNative jsvideo[key] = +value; */
		} else if (value instanceof Number || value instanceof Boolean) {
			/** @j2sNative jsvideo[key] = !+value */
		} else {
			/** @j2sNative jsvideo[key] = value; */
		}
	}

	/**
	 * Get a property using jsvideo[key], boxing number as Double and boolean as Boolean.
	 * 
	 * @param jsvideo the HTML5 video element
	 * 
	 * @param key
	 * @return  value or value boxed as Double or Boolean
	 */
	@SuppressWarnings("unused")
	public static Object getProperty(HTML5Video jsvideo, String key) {
		Object val = (/** @j2sNative 1? jsvideo[key] : */
		null);
		if (val == null)
			return null;
		switch (/** @j2sNative typeof val || */
		"") {
		case "number":
			return Double.valueOf(/** @j2sNative val || */
					0);
		case "boolean":
			return Boolean.valueOf(/** @j2sNative val || */
					false);
		default:
			return val;
		}
	}

	// event action
	
	/**
	 * Add an ActionListener for the designated events. When an event is fired, 
	 * 
	 * @param jsvideo the HTML5 video element
	 * @param listener
	 * @param events array of events to listen to or null to listen on all video element event types
	 * @return an array of event/listener pairs that can be used for removal. 
	 */
	public static Object[] addActionListener(HTML5Video jsvideo, ActionListener listener, String... events) {
		if (events == null || events.length == 0)
			events = eventTypes;
		@SuppressWarnings("unused")
		Function<Object, Void> f = new Function<Object, Void>() {

			@Override
			public Void apply(Object jsevent) {
				String name = (/** @j2sNative jsevent.type || */
				"?");
				ActionEvent e = new ActionEvent(new Object[] { jsvideo, jsevent }, 12345, name, System.currentTimeMillis(), 0);
				listener.actionPerformed(e);
				return null;
			}
		};
		Object[] listeners = new Object[0];
		for (int i = 0; i < events.length; i++) {
			/** @j2sNative 
			 * var func = function(event){f.apply.apply(f, [event])};
			 * jsvideo.addEventListener(events[i],func); 
			 * listeners.push(events[i]);
			 * listeners.push(func);
			 */
		}
		return listeners;
	}

	/**
	 * Remove action listener 
	 * 
	 * @param jsvideo the HTML5 video element
	 * @param listener
	 * @param events array of events to listen to or null to listen on all video element event types
	 */
	@SuppressWarnings("unused")
	public static void removeActionListener(HTML5Video jsvideo, Object[] listeners) {
		for (int i = 0; i < listeners.length; i+= 2) {
			String event = (String) listeners[i];
			Object listener = listeners[i + 1];
			/**
			 * @j2sNative
			 * 
			 * jsvideo.removeEventListener(event, listener);
			 */
		}
	}

	// HTMLMediaElement properties
	
//	audioTracks
//	autoplay
//	buffered Read only
//	controller
//	controls
//	controlsList Read only
//	crossOrigin
//	currentSrc Read only
//	currentTime
//	defaultMuted
//	defaultPlaybackRate
//	disableRemotePlayback
//	duration Read only
//	ended Read only
//	error Read only
//	loop
//	mediaGroup
//	mediaKeys Read only
//	mozAudioCaptured Read only
//	mozFragmentEnd
//	mozFrameBufferLength
//	mozSampleRate Read only
//	muted
//	networkState Read only
//	paused Read only
//	playbackRate
//	played Read only
//	preload
//	preservesPitch
//	readyState Read only
//	seekable Read only
//	seeking Read only
//	sinkId Read only
//	src
//	srcObject
//	textTracks Read only
//	videoTracks Read only
//	volume
//	initialTime Read only
//	mozChannels Read only

}
