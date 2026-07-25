package com.example.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import org.lwjgl.glfw.GLFW;

import java.util.Set;

public class NoLightPlaceClient implements ClientModInitializer {

    // Список источников света, которые будут блокироваться в левой руке
    private static final Set<Item> BLOCKED_LIGHT_ITEMS = Set.of(
            Items.TORCH,
            Items.SOUL_TORCH,
            Items.REDSTONE_TORCH,
            Items.LANTERN,
            Items.SOUL_LANTERN,
            Items.REDSTONE_LAMP,
            Items.GLOWSTONE,
            Items.SEA_LANTERN,
            Items.JACK_O_LANTERN,
            Items.SHROOMLIGHT,
            Items.OCHRE_FROGLIGHT,
            Items.VERDANT_FROGLIGHT,
            Items.PEARLESCENT_FROGLIGHT
    );

    private static KeyBinding lockKey;
    
    // ── Состояние тумблера: true = блокировка включена ──
    private static boolean isLocked = false;

    @Override
    public void onInitializeClient() {

        // Регистрация клавиши
        lockKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nolightplace.lock",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                "category.nolightplace.general"
        ));

        // ── Слушаем тики клиента, чтобы отлавливать одиночные нажатия ──
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // whilePressed() возвращает true каждый раз, когда клавиша была нажата,
            // и "съедает" это нажатие, чтобы оно не повторилось
            while (lockKey.wasPressed()) {
                isLocked = !isLocked; // переключаем состояние

                // Показываем сообщение над хотбаром
                if (client.player != null) {
                    String msg = isLocked 
                            ? "§cБлокировка света: ВКЛ" 
                            : "§aБлокировка света: ВЫКЛ";
                    client.player.sendMessage(Text.literal(msg), true);
                }
            }
        });

        // Перехват ПКМ по блоку (размещение)
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (shouldBlockPlacement(player, hand)) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        // Перехват ПКМ в воздух (использование предмета)
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (shouldBlockPlacement(player, hand)) {
                return TypedActionResult.fail(player.getStackInHand(hand));
            }
            return TypedActionResult.pass(player.getStackInHand(hand));
        });
    }

    private static boolean shouldBlockPlacement(PlayerEntity player, Hand hand) {
        if (hand != Hand.OFF_HAND) return false;
        if (!isLocked) return false;  // ← теперь проверяем состояние, а не зажатие

        ItemStack offhand = player.getOffHandStack();
        return BLOCKED_LIGHT_ITEMS.contains(offhand.getItem());
    }
}