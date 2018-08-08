package com.gunnarro.android.ughme;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;

import java.util.HashMap;

public class SoundManager {

    static private SoundManager instance;
    private static SoundPool soundPool;
    private static HashMap<Integer, Integer> soundPoolMap;
    private static AudioManager audioManager;
    private static Context context;

    private SoundManager() {
    }

    /**
     * Requests the instance of the Sound Manager and creates it if it does not
     * exist.
     *
     * @return Returns the single instance of the SoundManager
     */
    static synchronized public SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    /**
     * Initialises the storage for the sounds
     *
     * @param theContext The Application context
     */
    public static void initSounds(Context theContext) {
        Log.i("INFO", "init soundmanager...");
        context = theContext;
        soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);
        soundPoolMap = new HashMap<>();
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        loadSounds();
    }

    /**
     * Add a new Sound to the SoundPool
     *
     * @param Index   - The Sound Index for Retrieval
     * @param soundId - The Android ID for the Sound asset.
     */
    public static void addSound(int Index, int soundId) {
        soundPoolMap.put(Index, soundPool.load(context, soundId, 1));
    }

    /**
     * Loads the various sound assets Currently hardcoded but could easily be
     * changed to be flexible.
     */
    private static void loadSounds() {
        try {
            Log.i("INFO", "load sounds...");
            //	soundPoolMap.put(1, soundPool.load(context, R.raw.starwars, 1));
            //	soundPoolMap.put(2, soundPool.load(context, R.raw.terminator, 1));
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ERROR", e.getMessage());
        }
    }

    /**
     * Plays a Sound
     *
     * @param index - The Index of the Sound to be played
     * @param speed - The Speed to play not, not currently used but included for
     *              compatibility
     */
    public static void playSound(int index, float speed) {
        float streamVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        streamVolume = streamVolume / audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        soundPool.play(soundPoolMap.get(index), streamVolume, streamVolume, 1, 0, speed);
    }

    /**
     * Stop a Sound
     *
     * @param index - index of the sound to be stopped
     */
    public static void stopSound(int index) {
        soundPool.stop(soundPoolMap.get(index));
    }

    public static void cleanup() {
        soundPool.release();
        soundPool = null;
        soundPoolMap.clear();
        audioManager.unloadSoundEffects();
        instance = null;
    }
}
