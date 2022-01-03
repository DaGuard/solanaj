package org.p2p.solanaj.programs.anchor;

import org.p2p.solanaj.core.*;
import org.p2p.solanaj.programs.Program;
import org.p2p.solanaj.programs.SystemProgram;
import org.p2p.solanaj.rpc.Cluster;
import org.p2p.solanaj.rpc.RpcClient;
import org.p2p.solanaj.rpc.RpcException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Implements the "initialize" call from Anchor's basic-0 tutorial.
 */
public class AnchorBasicTutorialProgram extends Program {

    // Testnet address of basic-0 = EkEwddr34fqnv2SJREPynyC335PE32PAfjY4LVW5bTJS (has a method called initialize)
    private static final PublicKey INITIALIZE_PROGRAM_ID = new PublicKey("EkEwddr34fqnv2SJREPynyC335PE32PAfjY4LVW5bTJS");
    // Devnet address of multisig
    private static final PublicKey PROGRAM_ID = new PublicKey("2ZsZjFJXZmXUvSWqTtfGqKveMk8vWQ22FGFJZYjWtsdV");
    private static final String FUNCTION_NAMESPACE = "global::initialize";

    private static final int CREATE_METHOD_ID = 0;
    private static final int TRANSACTION_METHOD_ID = 1;

    public static Account ownerA;
    public static Account ownerB;
    public static Account ownerC;
    public static Account multisig;

    /**
     * Calls create_multisig
     *
     * takes in owners, threshold, nonce
     */
    public static TransactionInstruction createMultisig(PublicKey[] owners,long threshold, int nonce) {
        final List<AccountMeta> keys = new ArrayList<>();
        PublicKey SYSVAR_RENT_PUBKEY = new PublicKey("SysvarRent111111111111111111111111111111111");
        keys.add(new AccountMeta(multisig.getPublicKey(),true,true));
        keys.add(new AccountMeta(SYSVAR_RENT_PUBKEY, false, false));

        byte[] data =  encodeCreateMultisigInstructionData(owners,threshold,nonce);

        return createTransactionInstruction(
                PROGRAM_ID,
                keys,
                data
        );
    }
    public static TransactionInstruction createMultisigTransfer(PublicKey transferPid,AccountMeta[] accounts, byte[] data,Account transactionAccount) {
        final List<AccountMeta> keys = new ArrayList<>();
        PublicKey SYSVAR_RENT_PUBKEY = new PublicKey("SysvarRent111111111111111111111111111111111");
        keys.add(new AccountMeta(multisig.getPublicKey(),false,false));
        keys.add(new AccountMeta(transactionAccount.getPublicKey(),false,false));
        keys.add(new AccountMeta(ownerA.getPublicKey(),true,false));
        keys.add(new AccountMeta(SYSVAR_RENT_PUBKEY, false, false));



        byte[] transactionData =  encodeMultisigTransferInstructionData(transferPid,accounts,data);

        return createTransactionInstruction(
                PROGRAM_ID,
                keys,
                transactionData
        );
    }
    public static TransactionInstruction approveMultisigTransfer(Account transactionAccount) {
        final List<AccountMeta> keys = new ArrayList<>();
        PublicKey SYSVAR_RENT_PUBKEY = new PublicKey("SysvarRent111111111111111111111111111111111");
        keys.add(new AccountMeta(multisig.getPublicKey(),false,false));
        keys.add(new AccountMeta(transactionAccount.getPublicKey(),false,false));
        keys.add(new AccountMeta(ownerB.getPublicKey(),true,false));




        return createTransactionInstruction(
                PROGRAM_ID,
                keys,
                encodeApproveMultisigTransferInstructionData()
        );
    }
    public static TransactionInstruction executeMultisigTransaction(Account transactionAccount,PublicKey multisigSigner) {
        final List<AccountMeta> keys = new ArrayList<>();
        PublicKey SYSVAR_RENT_PUBKEY = new PublicKey("SysvarRent111111111111111111111111111111111");
        keys.add(new AccountMeta(multisig.getPublicKey(),false,true));
        keys.add(new AccountMeta(multisigSigner,false,true));
        keys.add(new AccountMeta(transactionAccount.getPublicKey(),false,true));
        //add remaining
        keys.add(new AccountMeta(ownerA.getPublicKey(),false,true));
        keys.add(new AccountMeta(PROGRAM_ID,false,true));




        return createTransactionInstruction(
                PROGRAM_ID,
                keys,
                encodeMultisigExecutionInstructionData()
        );
    }
    private static byte[] encodeMultisigExecutionInstructionData() {
        ByteBuffer result = ByteBuffer.allocate(15);
        result.order(ByteOrder.LITTLE_ENDIAN);
        result.put((byte)5);
        return result.array();
    }
    private static byte[] encodeApproveMultisigTransferInstructionData() {
        ByteBuffer result = ByteBuffer.allocate(15);
        result.order(ByteOrder.LITTLE_ENDIAN);
        result.put((byte)2);
        return result.array();
    }
    private static byte[] encodeMultisigTransferInstructionData(PublicKey transferPid,AccountMeta[] accounts, byte[] data) {
        ByteBuffer result = ByteBuffer.allocate(500);
        result.order(ByteOrder.LITTLE_ENDIAN);

        result.put((byte) TRANSACTION_METHOD_ID);
        result.put(transferPid.toByteArray());

        for(int i =0;i<accounts.length;i++){
            result.put(accounts[i].getPublicKey().toByteArray());
            result.put((byte) (accounts[i].isSigner()?1:0));
            result.put((byte) (accounts[i].isWritable()?1:0));

        }
        result.put(data);

        return result.array();
    }
    private static byte[] encodeCreateMultisigInstructionData(PublicKey[] owners,long threshold, int nonce) {
        ByteBuffer result = ByteBuffer.allocate(200);
        result.order(ByteOrder.LITTLE_ENDIAN);
    //how they encoded/ borsh encoding
        //test new one
        //has to do with signers last time
        result.put((byte) CREATE_METHOD_ID);
        Arrays.sort(owners, (idx1, idx2) -> idx1.toBase58().compareTo(idx2.toBase58()));
        for(int i =0;i<owners.length;i++){
            byte[] arr =owners[i].toByteArray();
            result.putInt(arr.length);
            result.put(arr);
        }
        //result.putInt((int)threshold);
        result.putLong(threshold);
        result.putInt(nonce);

        return result.array();
    }

    public static TransactionInstruction initialize(Account caller) {
        final List<AccountMeta> keys = new ArrayList<>();
        keys.add(new AccountMeta(caller.getPublicKey(),true, false));

        byte[] transactionData = encodeInitializeData();

        return createTransactionInstruction(
                INITIALIZE_PROGRAM_ID,
                keys,
                transactionData
        );
    }

    /**
     * Encodes the "global::initialize" sighash
     * @return byte array containing sighash for "global::initialize"
     */
    private static byte[] encodeInitializeData() {
        MessageDigest digest = null;
        byte[] encodedHash = null;
        int sigHashStart = 0;
        int sigHashEnd = 8;

        try {
            digest = MessageDigest.getInstance("SHA-256");
            encodedHash = Arrays.copyOfRange(
                    digest.digest(
                            FUNCTION_NAMESPACE.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    ),
                    sigHashStart,
                    sigHashEnd
            );
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return encodedHash;
    }

    public static void main(String[] args) throws Exception {
        RpcClient client = new RpcClient(Cluster.DEVNET);
        Account localAccount = new Account(new byte[]{67, -84, -89, -50, 65, 13, 68, 6, -21, 3, -26, 79, -89, 5, 69, -50, -28, 3, 24, -51, -99, 20, -49, 65, -34, -107, 94, -24, 47, -124, -74, -45, -57, -17, 27, -113, -2, 93, 63, -123, 107, -99, -123, 74, -77, 35, -2, 33, -94, -80, -6, -26, -83, 53, 18, 105, -13, -66, -72, -93, 39, 31, -5, 79});
        ownerA = new Account();
        ownerB = new Account();
        ownerC = new Account();
        long threshold = 2;
        PublicKey[] owners = new PublicKey[]{ownerA.getPublicKey(),ownerB.getPublicKey(),ownerC.getPublicKey()};
        multisig = new Account();
        PublicKey.ProgramDerivedAddress programAddress = PublicKey.findProgramAddress(Collections.singletonList(multisig.getPublicKey().toByteArray()), PROGRAM_ID);
        int nonce = programAddress.getNonce();
        PublicKey multisigSigner = programAddress.getAddress();
        System.out.println("Local Account: "+localAccount.getPublicKey());

        System.out.println("Owner A: "+ownerA.getPublicKey());
        System.out.println("Owner B: "+ownerB.getPublicKey());
        System.out.println("Owner C: "+ownerC.getPublicKey());

        System.out.println("Multisig Signer: " + multisigSigner);
        System.out.println("Multisig: " + multisig.getPublicKey());
        //creating multisig
        Transaction transaction = new Transaction();
        transaction.addInstruction(SystemProgram.createAccount(localAccount.getPublicKey(),multisig.getPublicKey(),client.getApi().getMinimumBalanceForRentExemption(200),200,PROGRAM_ID));
        transaction.addInstruction(
                createMultisig(owners,threshold,nonce)
        );
        List<Account> signers = List.of(localAccount,multisig);//or ownerA

        //sort the singers?????!!!


//        String result = null;
//        try {
//            result = client.getApi().sendTransaction(transaction, signers, null);
//        } catch (RpcException e) {
//            e.printStackTrace();
//        }
//        System.out.println(result);
//        transaction=new Transaction();



//        signers = List.of(multisig);
        String result = null;
        try {
            result = client.getApi().sendTransaction(transaction, signers, null);
        } catch (RpcException e) {
            e.printStackTrace();
        }
        System.out.println(result);

//        //funding the multisig
//        transaction = new Transaction();
//        transaction.addInstruction(
//                SystemProgram.transfer(localAccount.getPublicKey(),multisigSigner,100000000)
//        );
//        signers = List.of(localAccount);
//        result = null;
//        try {
//            result = client.getApi().sendTransaction(transaction, signers, null);
//        } catch (RpcException e) {
//            e.printStackTrace();
//        }
//        System.out.println(result);
//        //sending transfer from muiltisig to ownerA
//
//        Account transactionAccount = new Account();
//        transaction=new Transaction();
//
//        transaction.addInstruction(
//                SystemProgram.createAccount(localAccount.getPublicKey(),transactionAccount.getPublicKey(),client.getApi().getMinimumBalanceForRentExemption(1000),1000,PROGRAM_ID)
//        );
//
//        signers = List.of(transactionAccount,localAccount);
//        result = null;
//        try {
//            result = client.getApi().sendTransaction(transaction, signers, null);
//        } catch (RpcException e) {
//            e.printStackTrace();
//        }
//        System.out.println(result);
//        byte[] data = SystemProgram.transfer(multisigSigner,ownerA.getPublicKey(),50000000).getData();
//        PublicKey transferPid = new PublicKey("11111111111111111111111111111111");
//        AccountMeta[] transactionAccounts = new AccountMeta[]{new AccountMeta(multisigSigner,true,true),new AccountMeta(ownerA.getPublicKey(),false,true)};
//        transaction=new Transaction();
//
//        transaction.addInstruction(
//            createMultisigTransfer(transferPid,transactionAccounts,data,transactionAccount)
//        );
//
//        signers = List.of(ownerA,transactionAccount);
//        result = null;
//        try {
//            result = client.getApi().sendTransaction(transaction, signers, null);
//        } catch (RpcException e) {
//            e.printStackTrace();
//        }
//        System.out.println(result);
//        //approve transaction
//        transaction=new Transaction();
//
//        transaction.addInstruction(
//                approveMultisigTransfer(transactionAccount)
//        );
//
//        signers = List.of(ownerB);
//        result = null;
//        try {
//            result = client.getApi().sendTransaction(transaction, signers, null);
//        } catch (RpcException e) {
//            e.printStackTrace();
//        }
//        System.out.println(result);
//        //balance before 2.299955
//        //execute transactino
//        transaction=new Transaction();
//
//        //remaining accounts??
//        transaction.addInstruction(
//                executeMultisigTransaction(transactionAccount,multisigSigner)
//        );
//        //or owner A?
//        signers = List.of(multisig,ownerA);
//        result = null;
//        try {
//            result = client.getApi().sendTransaction(transaction, signers, null);
//        } catch (RpcException e) {
//            e.printStackTrace();
//        }
//        System.out.println(result);


    }


}
