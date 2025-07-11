package ab.noteblock_music_yoinker;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;

import net.minecraftforge.client.event.ScreenEvent.KeyPressed;
import net.minecraftforge.client.event.sound.PlaySoundSourceEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent.ClientTickEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// the value here should match an entry in the META-INF/mods.toml file
@Mod("noteblock_music_yoinker")
public class NoteblockMusicYoinker {
    // directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    // FIXME: can change this to KeyConflictContext.UNIVERSAL
    public static final Lazy<KeyMapping> RecordKey = Lazy
            .of(() -> new KeyMapping("key.noteblockmusicyoinker.togglerecording",
                    KeyConflictContext.IN_GAME,
                    KeyModifier.NONE,
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_R,
                    "key.categories.misc"));

    private static int time_passed = 0;
    private static boolean is_writing_to_file = false;
    private static String file_to_write;

    public NoteblockMusicYoinker() {
        // register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (is_writing_to_file) {
            ++time_passed;
        }

        if (event.phase == ClientTickEvent.Phase.END) {
            while (RecordKey.get().consumeClick()) {
                is_writing_to_file = !is_writing_to_file;

                if (is_writing_to_file) {
                    file_to_write = "recorded_music/" + java.time.LocalDateTime.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".csv";

                    // make a folder if there isn't one
                    File theDir = new File("recorded_music");

                    if (!theDir.exists()) {
                        theDir.mkdirs();
                    }

                    LOGGER.info("Started recording to file " + file_to_write + ".");
                } else {
                    time_passed = 0;
                    LOGGER.info("Stopped recording to file " + file_to_write + ".");
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onSoundPlayed(PlaySoundSourceEvent event) {
        String[] felbontott_nev = event.getName().split("[.]");

        // test if the sound is noteblock sound and if we're recording
        if (!felbontott_nev[0].equals("block") || !felbontott_nev[1].equals("note_block") || !is_writing_to_file) {
            return;
        }

        float pitch = event.getSound().getPitch();
        float volume = event.getSound().getVolume();
        LOGGER.info("Recording " + felbontott_nev[2] + " at time " + time_passed + " with pitch " + pitch
                + " and volume " + volume + ".");

        try {
            // we're opening and closing the file every time we add a line to it,
            // performance could be improved
            FileWriter myWriter = new FileWriter(file_to_write, true);
            myWriter.append(felbontott_nev[2] + "," + time_passed + "," + pitch + "," + volume + "\n");
            myWriter.close();
        } catch (IOException e) {
            LOGGER.error("error writing to file " + file_to_write);
            e.printStackTrace();
        }

        time_passed = 0;
    }

}
