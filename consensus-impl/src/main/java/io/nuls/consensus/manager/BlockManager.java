/*
 *
 * MIT License
 *
 * Copyright (c) 2017-2018 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.nuls.consensus.manager;

import io.nuls.account.entity.Address;
import io.nuls.consensus.cache.manager.block.BlockCacheBuffer;
import io.nuls.consensus.cache.manager.block.ConfirmingBlockCacheManager;
import io.nuls.consensus.cache.manager.tx.ConfirmingTxCacheManager;
import io.nuls.consensus.cache.manager.tx.OrphanTxCacheManager;
import io.nuls.consensus.cache.manager.tx.ReceivedTxCacheManager;
import io.nuls.consensus.constant.DownloadStatus;
import io.nuls.consensus.download.DownloadServiceImpl;
import io.nuls.consensus.download.DownloadUtils;
import io.nuls.consensus.entity.GetBlockParam;
import io.nuls.consensus.entity.block.BifurcateProcessor;
import io.nuls.consensus.entity.block.BlockHeaderChain;
import io.nuls.consensus.entity.block.BlockRoundData;
import io.nuls.consensus.entity.block.HeaderDigest;
import io.nuls.consensus.entity.tx.PocExitConsensusTransaction;
import io.nuls.consensus.entity.tx.PocJoinConsensusTransaction;
import io.nuls.consensus.entity.tx.RegisterAgentTransaction;
import io.nuls.consensus.event.GetBlockRequest;
import io.nuls.consensus.service.intf.BlockService;
import io.nuls.consensus.service.intf.DownloadService;
import io.nuls.consensus.service.tx.ExitConsensusTxService;
import io.nuls.consensus.service.tx.JoinConsensusTxService;
import io.nuls.consensus.service.tx.RegisterAgentTxService;
import io.nuls.core.chain.entity.Block;
import io.nuls.core.chain.entity.BlockHeader;
import io.nuls.core.chain.entity.NulsDigestData;
import io.nuls.core.chain.entity.Transaction;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.constant.TransactionConstant;
import io.nuls.core.constant.TxStatusEnum;
import io.nuls.core.context.NulsContext;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.utils.log.BlockLog;
import io.nuls.core.utils.log.Log;
import io.nuls.core.validate.ValidateResult;
import io.nuls.event.bus.service.intf.EventBroadcaster;
import io.nuls.ledger.service.intf.LedgerService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Niels
 * @date 2018/3/26
 */
public class BlockManager {

    private static final BlockManager INSTANCE = new BlockManager();
    private NulsContext context = NulsContext.getInstance();

    private ConfirmingBlockCacheManager confirmingBlockCacheManager = ConfirmingBlockCacheManager.getInstance();
    private BlockCacheBuffer blockCacheBuffer = BlockCacheBuffer.getInstance();

    private EventBroadcaster eventBroadcaster;
    private LedgerService ledgerService;

    private BifurcateProcessor bifurcateProcessor = BifurcateProcessor.getInstance();
    private ConfirmingTxCacheManager confirmingTxCacheManager = ConfirmingTxCacheManager.getInstance();
    private ReceivedTxCacheManager txCacheManager = ReceivedTxCacheManager.getInstance();
    private OrphanTxCacheManager orphanTxCacheManager = OrphanTxCacheManager.getInstance();

    private DownloadUtils downloadUtils = new DownloadUtils();


    private long storedHeight;
    private String lastAppravedHash;

    private BlockManager() {
    }

    public static BlockManager getInstance() {
        return INSTANCE;
    }

    public void init() {
        eventBroadcaster = NulsContext.getServiceBean(EventBroadcaster.class);
        ledgerService = NulsContext.getServiceBean(LedgerService.class);
    }

    public void addBlock(Block block, boolean verify, String nodeId) {
        if (block == null || block.getHeader() == null || block.getTxs() == null || block.getTxs().isEmpty()) {
            Log.warn("the block data error============================");
            return;
        }

        BlockRoundData roundData = new BlockRoundData(block.getHeader().getExtend());
        Log.debug("cache block:" + block.getHeader().getHash() +
                ",\nheight(" + block.getHeader().getHeight() + "),round(" + roundData.getRoundIndex() + "),index(" + roundData.getPackingIndexOfRound() + "),roundStart:" + roundData.getRoundStartTime());
        BlockHeader lastStoredBlockHeader = null;
        BlockService blockService = NulsContext.getServiceBean(BlockService.class);
        if (storedHeight == 0) {
            storedHeight = blockService.getLocalSavedHeight();
        }
        try {
            lastStoredBlockHeader = blockService.getBlockHeader(storedHeight);
        } catch (NulsException e) {
            Log.error(e);
        }
        if (null == lastStoredBlockHeader) {
            Log.warn("the lastStoredBlockHeader data error============================");
        }
        if (block.getHeader().getHeight() <= storedHeight) {
            Log.info("discard block height:" + block.getHeader().getHeight() + ", address:" + Address.fromHashs(block.getHeader().getPackingAddress()) + ",from:" + nodeId);
            return;
        }
        if (verify) {
            ValidateResult result = block.verify();
            if (result.isFailed() && result.getErrorCode() != ErrorCode.ORPHAN_BLOCK && result.getErrorCode() != ErrorCode.ORPHAN_TX) {
                Log.info("discard a block :" + result.getMessage());
                return;
            } else if (result.isFailed()) {
                cacheBlockToBuffer(block);
                return;
            }
        }
        boolean success = false;
        boolean canCache = true;
        if (confirmingBlockCacheManager.getHeaderCacheMap().isEmpty() && !block.getHeader().getPreHash().getDigestHex().equals(NulsContext.getInstance().getBestBlock().getHeader().getHash().getDigestHex())) {
            canCache = false;
        }
        if (!lastStoredBlockHeader.getHash().equals(block.getHeader().getPreHash()) && !confirmingBlockCacheManager.getHeaderCacheMap().containsKey(block.getHeader().getPreHash().getDigestHex())) {
            canCache = false;
        }
        if (canCache) {
            success = confirmingBlockCacheManager.cacheBlock(block);
        }
        if (!success) {
            cacheBlockToBuffer(block);
            return;
        }
        boolean needUpdateBestBlock = bifurcateProcessor.addHeader(block.getHeader());
        if (needUpdateBestBlock) {
            context.setBestBlock(block);
        }
        if (bifurcateProcessor.getChainSize() == 1) {
            try {
                this.appravalBlock(block);
                this.lastAppravedHash = block.getHeader().getHash().getDigestHex();
                checkNextblock(block.getHeader().getHash().getDigestHex());
            } catch (Exception e) {
                Log.error(e);
                confirmingBlockCacheManager.removeBlock(block.getHeader().getHash().getDigestHex());
                blockCacheBuffer.cacheBlock(block);
                return;
            }
        } else {
            Block lastAppravedBlock = confirmingBlockCacheManager.getBlock(lastAppravedHash);
            if (null != lastAppravedBlock) {
                this.rollbackAppraval(lastAppravedBlock);
            }
        }
        Set<String> keySet = blockCacheBuffer.getHeaderCacheMap().keySet();
        for (String key : keySet) {
            BlockHeader header = blockCacheBuffer.getBlockHeader(key);
            if (header == null) {
                continue;
            }
            if (header.getPreHash().getDigestHex().equals(block.getHeader().getHash())) {
                Block nextBlock = blockCacheBuffer.getBlock(key);
                this.addBlock(nextBlock, true, null);
            }
        }
    }

    private void cacheBlockToBuffer(Block block) {
        blockCacheBuffer.cacheBlock(block);
        BlockLog.debug("orphan cache block height:" + block.getHeader().getHeight() + ", preHash:" + block.getHeader().getPreHash() + " , hash:" + block.getHeader().getHash() + ", address:" + Address.fromHashs(block.getHeader().getPackingAddress()));
        Block preBlock = blockCacheBuffer.getBlock(block.getHeader().getPreHash().getDigestHex());
        if (preBlock == null) {
            if(NulsContext.getServiceBean(DownloadService.class).getStatus()!= DownloadStatus.DOWNLOADING){
                downloadUtils.getBlockByHash(block.getHeader().getPreHash().getDigestHex());
            }
        } else {
            this.addBlock(preBlock, true, null);
        }
    }

    public void appravalBlock(Block block) {
        for (int i = 0; i < block.getHeader().getTxCount(); i++) {
            Transaction tx = block.getTxs().get(i);
            tx.setBlockHeight(block.getHeader().getHeight());
            tx.setIndex(i);
            if (tx.getStatus() == null || tx.getStatus() == TxStatusEnum.CACHED) {
                try {
                    tx.verifyWithException();
                    this.ledgerService.approvalTx(tx);
                } catch (NulsException e) {
                    rollbackTxList(block.getTxs(), 0, i);
                    Log.error(e);
                    throw new NulsRuntimeException(e);
                }
            } else if (tx.getType() == TransactionConstant.TX_TYPE_REGISTER_AGENT) {
                NulsContext.getServiceBean(RegisterAgentTxService.class).onApproval((RegisterAgentTransaction) tx);
            } else if (tx.getType() == TransactionConstant.TX_TYPE_JOIN_CONSENSUS) {
                NulsContext.getServiceBean(JoinConsensusTxService.class).onApproval((PocJoinConsensusTransaction) tx);
            } else if (tx.getType() == TransactionConstant.TX_TYPE_EXIT_CONSENSUS) {
                NulsContext.getServiceBean(ExitConsensusTxService.class).onApproval((PocExitConsensusTransaction) tx);
            }
            confirmingTxCacheManager.putTx(tx);
        }
        txCacheManager.removeTx(block.getTxHashList());
        orphanTxCacheManager.removeTx(block.getTxHashList());
    }

    private void rollbackTxList(List<Transaction> txList, int start, int end) {
        List<NulsDigestData> txHashList = new ArrayList<>();
        for (int i = start; i <= end && i < txList.size(); i++) {
            Transaction tx = txList.get(i);
            if (tx.getStatus() == TxStatusEnum.AGREED) {
                try {
                    ledgerService.rollbackTx(tx);

                } catch (NulsException e) {
                    Log.error(e);
                }
                txHashList.add(tx.getHash());
            }
        }
        confirmingTxCacheManager.removeTxList(txHashList);

    }

    private void rollbackAppraval(Block block) {
        if (null == block) {
            Log.warn("the block is null!");
            return;
        }
        this.rollbackTxList(block.getTxs(), 0, block.getTxs().size());
        this.lastAppravedHash = block.getHeader().getPreHash().getDigestHex();
        Block preBlock = this.getBlock(lastAppravedHash);
        List<String> hashList = this.bifurcateProcessor.getAllHashList(block.getHeader().getHeight() - 1);
        if (hashList.size() > 1) {
            this.rollbackAppraval(preBlock);
        }
    }

    public Block getBlock(long height) {
        String hash = this.bifurcateProcessor.getBlockHash(height);
        if (hash != null) {
            return getBlock(hash);
        }
        return null;
    }

    public Block getBlock(String hash) {
        Block block = confirmingBlockCacheManager.getBlock(hash);
        if (block == null) {
            block = this.blockCacheBuffer.getBlock(hash);
        }
        return block;
    }

    public void rollback(Block block) {
        this.rollbackAppraval(block);
    }


    private void checkNextblock(String hash) {
        Set<String> nextHashSet = blockCacheBuffer.getNextHash(hash);
        if (null == nextHashSet || nextHashSet.isEmpty()) {
            return;
        }
        for (String nextHash : nextHashSet) {
            Block block = blockCacheBuffer.getBlock(nextHash);
            if (null == block) {
                return;
            }
            blockCacheBuffer.removeBlock(nextHash);
            this.addBlock(block, true, null);
        }
    }

    public long getStoredHeight() {
        return storedHeight;
    }

    public boolean processingBifurcation(long height) {
        return this.bifurcateProcessor.processing(height);
    }

    public void storedBlock(Block storedBlock) {
        this.storedHeight = storedBlock.getHeader().getHeight();
        List<String> hashList = this.bifurcateProcessor.getAllHashList(storedBlock.getHeader().getHeight());
        String storedHash = storedBlock.getHeader().getHash().getDigestHex();
        if (hashList.size() == 1) {
            this.bifurcateProcessor.removeHash(storedHash);
        }
        confirmingBlockCacheManager.removeBlock(storedHash);
        blockCacheBuffer.removeBlock(storedHash);

    }

    public void removeBlock(String hash) {
        this.bifurcateProcessor.removeHash(hash);
        confirmingBlockCacheManager.removeBlock(hash);
        blockCacheBuffer.removeBlock(hash);
    }

    public BlockHeader getBlockHeader(String hashHex) {
        BlockHeader header = confirmingBlockCacheManager.getBlockHeader(hashHex);
        if (null == header) {
            header = blockCacheBuffer.getBlockHeader(hashHex);
        }
        return header;
    }

    public BlockHeader getBlockHeader(long height) {
        String hash = this.bifurcateProcessor.getBlockHash(height);
        if (hash == null) {
            return null;
        }
        return confirmingBlockCacheManager.getBlockHeader(hash);
    }

    public void clear() {
        this.bifurcateProcessor.clear();
        this.confirmingBlockCacheManager.clear();
        this.blockCacheBuffer.clear();

    }

    public Block getHighestBlock() {
        BlockHeaderChain chain = bifurcateProcessor.getApprovingChain();
        if (null == chain) {
            return null;
        }
        HeaderDigest headerDigest = chain.getLastHd();
        if(null==headerDigest){
            return null;
        }
        return this.getBlock(headerDigest.getHash());
    }

    public Block getBlockFromMyChain(long start) {
        if(null==bifurcateProcessor||null==bifurcateProcessor.getApprovingChain()){
            return null;
        }
        HeaderDigest hd = this.bifurcateProcessor.getApprovingChain().getHeaderDigest(start);
        if (null == hd) {
            return null;
        }
        return getBlock(hd.getHash());
    }

    public Block getBlockFromMyChain(String hash) {
        if(null==bifurcateProcessor||null==bifurcateProcessor.getApprovingChain()){
            return null;
        }
        HeaderDigest hd = this.bifurcateProcessor.getApprovingChain().getHeaderDigest(hash);
        if (null == hd) {
            return null;
        }
        return getBlock(hash);
    }
}
