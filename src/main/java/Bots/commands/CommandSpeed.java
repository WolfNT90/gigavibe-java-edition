package Bots.commands;

import Bots.BaseCommand;
import Bots.CommandStateChecker.Check;
import Bots.CommandEvent;
import Bots.lavaplayer.GuildMusicManager;
import Bots.lavaplayer.PlayerManager;
import com.github.natanbc.lavadsp.timescale.TimescalePcmAudioFilter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import static Bots.Main.*;

public class CommandSpeed extends BaseCommand {
    @Override
    public Check[] getChecks() {
        return new Check[]{Check.IS_DJ, Check.IS_IN_SAME_VC, Check.IS_PLAYING};
    }

    @Override
    public void execute(CommandEvent event) {
        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuild());
        TimescalePcmAudioFilter timescale = (TimescalePcmAudioFilter) musicManager.filters.get(AudioFilters.Timescale); // this needs to be redefined many times due to how it works.

        if (event.getArgs().length == 1) {
            timescale.setSpeed(1);
            event.replyEmbeds(createQuickEmbed("✅ **Success**", "Set the speed back to 1."));
            return;
        }

        float value = Float.parseFloat(String.format("%.3f %n", Float.parseFloat(event.getArgs()[1])));

        if (!(value <= 5 && value >= 0.2)) {
            event.replyEmbeds(createQuickError("This speed must be between 0.2 and 5"));
            return;
        }

        timescale.setSpeed(value);
        event.replyEmbeds(createQuickEmbed("✅ **Success**", "Set the playback speed of the track to " + value + "x."));
    }

    @Override
    public void ProvideOptions(SlashCommandData slashCommand) {
        slashCommand.addOption(OptionType.NUMBER, "tempo", "The speed at which the track will playback, supports decimals.", false);
    }

    @Override
    public String getOptions() {
        return "<Number>";
    }

    @Override
    public Category getCategory() {
        return Category.DJ;
    }

    @Override
    public String[] getNames() {
        return new String[]{"speed", "tempo"};
    }

    @Override
    public String getDescription() {
        return "Changes the speed/tempo of the song.";
    }

    @Override
    public long getRatelimit() {
        return 2000;
    }
}
