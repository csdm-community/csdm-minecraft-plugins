package tv.csdm.minecraft.verify;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;
import tv.csdm.minecraft.verify.listener.PlayerProtectionListener;
import tv.csdm.minecraft.verify.world.WorldRoutingService;

class VerificationSafetyTest {
    private final World verification = mock(World.class);
    private final World museum = mock(World.class);
    private final Player player = mock(Player.class);
    private final WorldRoutingService routing = mock(WorldRoutingService.class);
    private final PlayerProtectionListener listener = new PlayerProtectionListener(true, routing);
    private final Location spawn = new Location(verification, 0.5, 101, 0.5);

    VerificationSafetyTest() {
        when(player.getWorld()).thenReturn(verification);
        when(routing.isVerificationWorld(verification)).thenReturn(true);
        when(routing.verificationSpawn()).thenReturn(spawn);
    }

    @Test void fallingReturnsToVerificationAndClearsMomentum() {
        var event = new PlayerMoveEvent(player, new Location(verification, 8, 92, 8),
                new Location(verification, 8, 90, 8));
        listener.onMove(event);
        assertEquals(spawn, event.getTo());
        verify(player).setFallDistance(0);
        verify(player).setVelocity(new Vector());
    }

    @Test void thresholdFollowsPlatformHeight() {
        when(routing.verificationSpawn()).thenReturn(new Location(verification, 0.5, 25, 0.5));
        var safe = new Location(verification, 2, 20, 2);
        var event = new PlayerMoveEvent(player, safe, safe);
        listener.onMove(event);
        assertSame(safe, event.getTo());
        event.setTo(new Location(verification, 2, 14, 2));
        listener.onMove(event);
        assertEquals(25, event.getTo().getY());
    }

    @Test void museumMovementIsNotIntercepted() {
        var destination = new Location(museum, 0, -100, 0);
        var event = new PlayerMoveEvent(player, destination, destination);
        listener.onMove(event);
        assertSame(destination, event.getTo());
        verify(player, never()).setFallDistance(anyFloat());
    }

    @Test void staffAlsoReceivesNoDamageAndVoidDamageRescues() {
        when(player.hasPermission("csdm.verify.bypass-protection")).thenReturn(true);
        for (var cause : new EntityDamageEvent.DamageCause[] {
                EntityDamageEvent.DamageCause.FALL, EntityDamageEvent.DamageCause.VOID,
                EntityDamageEvent.DamageCause.FIRE}) {
            var event = mock(EntityDamageEvent.class);
            when(event.getEntity()).thenReturn(player);
            when(event.getCause()).thenReturn(cause);
            listener.onDamage(event);
            verify(event).setCancelled(true);
        }
        verify(player).teleport(spawn);
    }

    @Test void damageOutsideVerificationIsUnchanged() {
        when(player.getWorld()).thenReturn(museum);
        var event = mock(EntityDamageEvent.class);
        when(event.getEntity()).thenReturn(player);
        listener.onDamage(event);
        verify(event, never()).setCancelled(anyBoolean());
    }

    @Test void deathInVerificationCannotRespawnAtMuseum() {
        var event = mock(PlayerRespawnEvent.class);
        when(event.getPlayer()).thenReturn(player);
        listener.onRespawn(event);
        verify(event).setRespawnLocation(spawn);
        when(player.getWorld()).thenReturn(museum);
        var other = mock(PlayerRespawnEvent.class);
        when(other.getPlayer()).thenReturn(player);
        listener.onRespawn(other);
        verify(other, never()).setRespawnLocation(any());
    }
}
