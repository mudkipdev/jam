package jam.utility.handler;

import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;

public final class SignHandler implements BlockHandler {
    public static final NamespaceID KEY = NamespaceID.from("minecraft:sign");

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return NamespaceID.from(KEY);
    }

    @Override
    public @NotNull Collection<Tag<?>> getBlockEntityTags() {
        return Set.of(
                Tag.Byte("is_waxed"),
                Tag.NBT("front_text"),
                Tag.NBT("back_text"));
    }
}
