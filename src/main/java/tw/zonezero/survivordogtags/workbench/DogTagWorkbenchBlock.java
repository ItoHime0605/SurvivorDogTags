package tw.zonezero.survivordogtags.workbench;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Compact survival-era engraving bench. Faces the player on placement.
 * No persistent block entity: all inputs are returned on GUI close.
 */
public final class DogTagWorkbenchBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<DogTagWorkbenchBlock> CODEC = simpleCodec(DogTagWorkbenchBlock::new);

    @Override
    protected MapCodec<DogTagWorkbenchBlock> codec() {
        return CODEC;
    }

    public DogTagWorkbenchBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    // The non-cubic model must not retain a full-block phantom collision box.
    // Shape details rotate together with the block's FACING property.
    private static final VoxelShape[] SHAPES = new VoxelShape[4];
    static {
        VoxelShape north = Shapes.or(
            box(0, 0, 0, 16, 13, 16),   // metal bench body and work surface
            box(1, 13, 13, 15, 16, 15), // rear guard
            box(2, 13, 3, 6, 15, 8),    // holding vise
            box(10, 13, 4, 14, 16, 7)   // engraver column
        );
        SHAPES[0] = north;
        for (int rotation = 1; rotation < SHAPES.length; rotation++) {
            SHAPES[rotation] = rotateShape(north, rotation);
        }
    }

    /** Rotate bounding boxes around Y; 0=N, 1=E, 2=S, 3=W. */
    private static VoxelShape rotateShape(VoxelShape base, int quarterTurns) {
        VoxelShape result = Shapes.empty();
        for (AABB aabb : base.toAabbs()) {
            double x1 = aabb.minX, x2 = aabb.maxX;
            double z1 = aabb.minZ, z2 = aabb.maxZ;
            for (int i = 0; i < quarterTurns; i++) {
                double nx1 = z1, nx2 = z2;
                z1 = 1.0 - x2;
                z2 = 1.0 - x1;
                x1 = nx1;
                x2 = nx2;
            }
            result = Shapes.or(result, Shapes.box(x1, aabb.minY, z1, x2, aabb.maxY, z2));
        }
        return result.optimize();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int facingIndex = switch (state.getValue(FACING)) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        return SHAPES[facingIndex];
    }

    private void openWorkbench(Level world, BlockPos pos, Player player) {
        if (!world.isClientSide() && player instanceof ServerPlayer server) {
            server.openMenu(new SimpleMenuProvider((id, inv, user) ->
                new DogTagWorkbenchMenu(id, inv, ContainerLevelAccess.create(world, pos)),
                Component.translatable("menu.survivordogtags.dog_tag_workbench")),
                data -> data.writeBlockPos(pos));
        }
    }

    /** Holding a photograph or dog tag must open the bench, not activate the held item. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level world, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        openWorkbench(world, pos, player);
        return ItemInteractionResult.sidedSuccess(world.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        openWorkbench(world, pos, player);
        return InteractionResult.sidedSuccess(world.isClientSide());
    }
}
