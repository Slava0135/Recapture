import arc.Events;
import arc.util.Timer;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.mod.Plugin;
import mindustry.world.blocks.storage.*;

public class Recapture extends Plugin {
    @Override
    public void init() {
        Events.on(EventType.BlockDestroyEvent.class, e -> {
            if (e.tile.build instanceof CoreBlock.CoreBuild){
                var tile = e.tile;
                var oldTeam = e.tile.build.team;
                var block = e.tile.build.block;
                var closestEnemy = Units.closestEnemy(oldTeam, tile.worldx(), tile.worldy(), 10000000, u -> true);
                var newTeam = closestEnemy != null ? closestEnemy.team : Team.derelict;
                Call.effectReliable(Fx.upgradeCore, tile.build.x, tile.build.y, block.size, newTeam.color);
                Timer.schedule(() -> tile.setNet(block, newTeam, 0), 0.6f);
            }
        });
    }
}
