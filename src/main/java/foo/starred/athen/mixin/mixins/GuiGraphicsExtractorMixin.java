package foo.starred.athen.mixin.mixins;

import foo.starred.athen.events.GuiEvent;
import foo.starred.athen.modules.impl.render.ContainerScale;
import foo.starred.athen.modules.impl.render.tooltip.ScrollableTooltip;
import foo.starred.athen.modules.impl.render.tooltip.custom.CustomTooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
//~ if >= 26.2 'Quaternionf' -> 'Quaternionfc'
import org.joml.Quaternionf;
//~ if >= 26.2 'Vector3f' -> 'Vector3fc'
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = GuiGraphicsExtractor.class, priority = Integer.MAX_VALUE)
public class GuiGraphicsExtractorMixin {
    @Inject(method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V", at = @At("HEAD"))
    private void athen$renderItem(LivingEntity owner, Level level, ItemStack itemStack, int x, int y, int seed, CallbackInfo ci) {
        new GuiEvent.Items.Render.Pre(self(), itemStack, x, y).post();
    }

    @Inject(method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("TAIL"))
    private void athen$renderItemDecorations(Font font, ItemStack itemStack, int x, int y, String countText, CallbackInfo ci) {
        new GuiEvent.Items.Render.Post(self(), itemStack, x, y).post();
    }

    @Inject(method = "tooltip", at = @At("HEAD"), cancellable = true)
    private void athen$renderTooltip(Font font, List<ClientTooltipComponent> lines, int x, int y, ClientTooltipPositioner positioner, Identifier style, CallbackInfo ci) {
        boolean a = CustomTooltip.INSTANCE.getEnabled();
        boolean b = ScrollableTooltip.INSTANCE.getEnabled();

        if (!a && !b) return;
        ci.cancel();

        if (a) CustomTooltip.render(self(), font, lines, x, y, positioner);
        else ScrollableTooltip.fn(self(), font, lines, x, y, positioner, style);
    }

    @Inject(method = "entity", at = @At("HEAD"), cancellable = true)
    //~ if >= 26.2 'Vector3f' -> 'Vector3fc'
    //~ if >= 26.2 'Quaternionf' -> 'Quaternionfc'
    private void athen$entity(EntityRenderState renderState, float scale, Vector3f translation, Quaternionf rotation, Quaternionf overrideCameraAngle, int x0, int y0, int x1, int y1, CallbackInfo ci) {
        if (!ContainerScale.getBool()) return;

        double scale1 = ContainerScale.INSTANCE.getScale();
        double x2 = ContainerScale.getX0();
        double y2 = ContainerScale.getY0();

        int x3 = (int) (x2 + (x0 - x2) * scale1);
        int y3 = (int) (y2 + (y0 - y2) * scale1);
        int x4 = (int) (x2 + (x1 - x2) * scale1);
        int y4 = (int) (y2 + (y1 - y2) * scale1);
        float scale2 = (float) (scale * scale1);

        renderState.lightCoords = 15728880;
        self().guiRenderState.addPicturesInPictureState(new GuiEntityRenderState(renderState, translation, rotation, overrideCameraAngle, x3, y3, x4, y4, scale2, self().scissorStack.peek()));

        ci.cancel();
    }

    @Unique
    private GuiGraphicsExtractor self() {
        return (GuiGraphicsExtractor) (Object) this;
    }
}
