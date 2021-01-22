import arc.Events;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.mod.Plugin;
import mindustry.world.Edges;
import mindustry.world.blocks.storage.*;

public class Recapture extends Plugin {
    @Override
    public void init() {
        Events.on(EventType.BlockDestroyEvent.class, e -> {
            if (e.tile.build instanceof CoreBlock.CoreBuild){
                var tile = e.tile;
                var block = e.tile.build.block;
                Timer.schedule(() -> {
                    var closestEnemy = Units.closestEnemy(tile.team(), tile.x, tile.y, block.size * 4, u -> true);
                    tile.setBlock(block, closestEnemy != null ? closestEnemy.team : Team.derelict);
                }, 0.1f);
            }
        });
    }
}
