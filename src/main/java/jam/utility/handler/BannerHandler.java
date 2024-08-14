package jam.utility.handler;

import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;

public final class BannerHandler implements BlockHandler {
    public static final NamespaceID KEY = NamespaceID.from("minecraft:banner");

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return KEY;
    }

    @Override
    public @NotNull Collection<Tag<?>> getBlockEntityTags() {
        return Set.of(
                Tag.NBT("patterns"));
    }
}
