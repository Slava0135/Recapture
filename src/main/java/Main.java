import arc.Events;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.mod.Plugin;
import mindustry.world.Edges;
import mindustry.world.blocks.storage.*;

public class Main extends Plugin {
    @Override
    public void init() {
        Events.on(EventType.BlockDestroyEvent.class, e -> {
            if (e.tile.build instanceof CoreBlock.CoreBuild){
                var tile = e.tile;
                var block = e.tile.build.block;
                Timer.schedule(() -> {
                    tile.setBlock(block, Team.derelict); //replace destroyed core with new derelict one after delay
                }, 0.1f);
            }
        });

        Timer.schedule(() -> {
            for (var team : Vars.state.teams.active) {
                for (var core : team.cores) {
                    var enemyBlocks = 0;
                    Team firstEnemy = null;
                    for (var edge : Edges.getEdges(core.block.size)) {
                        var nearby = Vars.world.tileBuilding(core.tile.x + edge.x, core.tile.y + edge.y);

                        if (nearby.team() == core.team) break;

                        if (firstEnemy == null || firstEnemy == nearby.team()) {
                            enemyBlocks++;
                            firstEnemy = nearby.team();
                        } else break;
                    }
                    if(enemyBlocks == core.block.size * 4){
                        core.team = firstEnemy;
                    }
                }
            }
        }, 0, 0.1f);
    }
}
