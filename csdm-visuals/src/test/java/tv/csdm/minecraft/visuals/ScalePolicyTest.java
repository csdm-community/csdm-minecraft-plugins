package tv.csdm.minecraft.visuals;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScalePolicyTest {
    @Test void normalPlayerIsLargeToSelfAndSmallToEveryoneElse() {
        assertEquals(1, ScalePolicy.scale(true, false, .3));
        assertEquals(.3, ScalePolicy.scale(false, false, .3));
    }
    @Test void personalidadIsFullSizeForEveryObserver() {
        assertEquals(1, ScalePolicy.scale(false, true, .3));
        assertEquals(1, ScalePolicy.scale(true, true, .3));
    }
}
