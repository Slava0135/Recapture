import arc.Events;
import arc.util.Timer;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.mod.Plugin;
import mindustry.world.blocks.storage.*;

public class Main extends Plugin {
    @Override
    public void init() {
        Events.on(EventType.BlockDestroyEvent.class, e -> {
            if (e.tile.build instanceof CoreBlock.CoreBuild){
                var tile = e.tile;
                var block = e.tile.build.block;
                Timer.schedule(() -> {
                    tile.setBlock(block, Team.derelict);
                }, 0.1f);
            }
        });
    }
}
