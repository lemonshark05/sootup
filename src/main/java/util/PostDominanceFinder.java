package util;

import sootup.core.graph.BasicBlock;
import sootup.core.graph.StmtGraph;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @see <a
 *     href="https://www.researchgate.net/publication/2569680_A_Simple_Fast_Dominance_Algorithm">
 *     https://www.researchgate.net/publication/2569680_A_Simple_Fast_Dominance_Algorithm </a>
 */
public class PostDominanceFinder {

  private List<BasicBlock<?>> blocks;
  private Map<BasicBlock<?>, Integer> blockToIdx = new HashMap<>();
  private int[] doms;
  private ArrayList<Integer>[] domFrontiers;

  public PostDominanceFinder(StmtGraph<?> blockGraph) {

    blocks = StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(
                            blockGraph.getBlockIterator(), Spliterator.ORDERED),
                    false)
            .collect(Collectors.toList());

    final BasicBlock<?> startingStmtBlock = blockGraph.getBlockOf(blockGraph.getTails().get(0));

    for (int i = 0; i < blocks.size(); i++) {
      BasicBlock<?> block = blocks.get(i);
      blockToIdx.put(block, i);
    }

    // initialize domains
    doms = new int[blocks.size()];
    doms[0] = 0;
    for (int i = 1; i < doms.length; i++) {
      doms[i] = -1;
    }

    // calculate immediate dominator for each block
    boolean isChanged = true;
    while (isChanged) {
      isChanged = false;
      for (BasicBlock<?> block : blocks) {
        if (block.equals(startingStmtBlock)) {
          continue;
        }
        int blockIdx = blockToIdx.get(block);
        List<BasicBlock<?>> preds = new ArrayList<>(block.getSuccessors());
        // ms: should not be necessary preds.addAll(block.getExceptionalPredecessors());
        int newIdom = getFirstDefinedBlockPredIdx(preds);
        if (!preds.isEmpty() && newIdom != -1) {
          preds.remove(blocks.get(newIdom));
          for (BasicBlock<?> pred : preds) {
            int predIdx = blockToIdx.get(pred);
            if (this.doms[predIdx] != -1) {
              newIdom = isIntersecting(newIdom, predIdx);
            }
          }
          if (doms[blockIdx] != newIdom) {
            doms[blockIdx] = newIdom;
            isChanged = true;
          }
        }
      }
    }

    // initialize domFrontiers
    domFrontiers = new ArrayList[blockGraph.getBlocks().size()];
    for (int i = 0; i < domFrontiers.length; i++) {
      domFrontiers[i] = new ArrayList<>();
    }

    // calculate dominance frontiers for each block
    for (BasicBlock<?> block : blocks) {
      List<BasicBlock<?>> preds = new ArrayList<>(block.getSuccessors());
      // ms: should not be necessary  preds.addAll(block.getExceptionalPredecessors());
      if (preds.size() > 1) {
        int blockId = blockToIdx.get(block);
        for (BasicBlock<?> pred : preds) {
          int predId = blockToIdx.get(pred);
          while (predId != -1 && predId != doms[blockId]) {
            domFrontiers[predId].add(blockId);
            predId = doms[predId];
          }
        }
      }
    }
  }

  public void replaceBlock(@Nonnull BasicBlock<?> newBlock, BasicBlock<?> oldBlock) {
    if (!blockToIdx.containsKey(oldBlock)) {
      throw new RuntimeException("The given block: " + oldBlock + " is not in BlockGraph!");
    }
    final Integer idx = blockToIdx.get(oldBlock);
    blockToIdx.put(newBlock, idx);
    blockToIdx.remove(oldBlock);
    blocks.set(idx, newBlock);
  }

  @Nonnull
  public BasicBlock<?> getImmediateDominator(@Nonnull BasicBlock<?> block) {
    if (!blockToIdx.containsKey(block)) {
      throw new RuntimeException("The given block: " + block + " is not in BlockGraph!");
    }
    int idx = blockToIdx.get(block);
    int idomIdx = this.doms[idx];
    if (idomIdx < 0) {
      return null;
    }
    return blocks.get(idomIdx);
  }

  @Nonnull
  public Set<BasicBlock<?>> getDominanceFrontiers(@Nonnull BasicBlock<?> block) {
    if (!blockToIdx.containsKey(block)) {
      throw new RuntimeException("The given block: " + block + " is not in BlockGraph!");
    }
    int idx = blockToIdx.get(block);
    Set<BasicBlock<?>> dFs = new HashSet<>();
    ArrayList<Integer> dFs_idx = this.domFrontiers[idx];
    for (Integer i : dFs_idx) {
      dFs.add(blocks.get(i));
    }
    return dFs;
  }

  @Nonnull
  public List<BasicBlock<?>> getIdxToBlock() {
    return blocks;
  }

  @Nonnull
  public Map<BasicBlock<?>, Integer> getBlockToIdx() {
    return blockToIdx;
  }

  @Nonnull
  public int[] getImmediateDominators() {
    return this.doms;
  }

  private int getFirstDefinedBlockPredIdx(List<BasicBlock<?>> preds) {
    for (BasicBlock<?> block : preds) {
      int idx = blockToIdx.get(block);
      if (doms[idx] != -1) {
        return idx;
      }
    }
    return -1;
  }

  private int isIntersecting(int a, int b) {
    while (a != b) {
      if (a > b) {
        a = doms[a];
      } else {
        b = doms[b];
      }
    }
    return a;
  }
}
