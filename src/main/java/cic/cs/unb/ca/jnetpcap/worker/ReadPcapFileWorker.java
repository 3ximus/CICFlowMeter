package cic.cs.unb.ca.jnetpcap.worker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingWorker;

import cic.cs.unb.ca.jnetpcap.FlowFeature;
import org.apache.commons.io.FilenameUtils;
import org.jnetpcap.PcapClosedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cic.cs.unb.ca.jnetpcap.BasicPacketInfo;
import cic.cs.unb.ca.jnetpcap.FlowGenerator;
import cic.cs.unb.ca.jnetpcap.PacketReader;

import static cic.cs.unb.ca.Sys.FILE_SEP;


public class ReadPcapFileWorker extends SwingWorker<List<String>,String>{

    public static final Logger logger = LoggerFactory.getLogger(ReadPcapFileWorker.class);
    private static final String DividingLine = "----------------------------------------------------------------------------";

    private PacketReader    packetReader;
    private BasicPacketInfo basicPacket = null;
    private FlowGenerator   flowGen; //15000 useconds = 15ms.///////////////8
    private long flowTimeout;
    private long activityTimeout;
    private boolean readIP6 = false;
    private boolean readIP4 = true;
    private int     totalFlows = 0;

    private File pcapPath;
    private String outPutDirectory;
    private List<String> chunks;

    public ReadPcapFileWorker(File inputFile, String outPutDir) {
        super();
        pcapPath = inputFile;
        outPutDirectory = outPutDir;
        chunks = new ArrayList<>();

        if(!outPutDirectory.endsWith(FILE_SEP)) {
            outPutDirectory = outPutDirectory + FILE_SEP;
        }
        flowTimeout = 120000000L;
        activityTimeout = 5000000L;
    }

    public ReadPcapFileWorker(File inputFile, String outPutDir,long param1,long param2) {
        super();
        pcapPath = inputFile;
        outPutDirectory = outPutDir;
        chunks = new ArrayList<>();

        if(!outPutDirectory.endsWith(FILE_SEP)) {
            outPutDirectory = outPutDirectory + FILE_SEP;
        }
        flowTimeout = param1;
        activityTimeout = param2;
    }

    @Override
    protected List<String> doInBackground() throws Exception {

        if (pcapPath.isDirectory()) {
            readPcapDir(pcapPath,outPutDirectory);
        } else {

            if (!pcapPath.getName().toLowerCase().endsWith("pcap")) {
                publish("Please select pcap file!");
                publish("");
            } else {
                publish("CICFlowMeter received 1 pcap file");
                publish("");
                publish("");
                readPcapFile(pcapPath.getPath(), outPutDirectory);
            }
        }
        chunks.clear();
        chunks.add("");
        chunks.add(DividingLine);
        chunks.add(String.format("TOTAL FLOWS GENERATED :%s", totalFlows));
        chunks.add(DividingLine);
        publish(chunks.toArray( new String[chunks.size()]));

        return chunks;
    }

    @Override
    protected void done() {
        super.done();
    }

    @Override
    protected void process(List<String> chunks) {
        super.process(chunks);
        firePropertyChange("progress","",chunks);
    }

    private void readPcapDir(File inputPath, String outPath) {
        if(inputPath==null||outPath==null) {
            return;
        }

        File[] pcapFiles = inputPath.listFiles(file -> file.getName().toLowerCase().endsWith("pcap"));

        publish(String.format("CICFlowMeter found :%s pcap files", pcapFiles.length));
        publish("");
        publish("");

        for(File file:pcapFiles) {
            readPcapFile(file.getPath(),outPath);
        }
    }

    public void readPcapFile(String inputFile, String outPath) {

        if(inputFile==null ||outPath==null ) {
            return;
        }

        String nameWithoutExt = FilenameUtils.getBaseName(inputFile);

        flowGen = new FlowGenerator(true,flowTimeout, activityTimeout);
        packetReader = new PacketReader(inputFile,readIP4,readIP6);

        publish(String.format("Working on... %s",inputFile));
        logger.debug("Working on... {}",inputFile);

        int nValid=0;
        int nTotal=0;
        int nDiscarded = 0;
        long start = System.currentTimeMillis();
        while(true) {
            // System.out.println('#' + String.valueOf(nTotal));
            try{
                basicPacket = packetReader.nextPacket();
                nTotal++;
                if(basicPacket!=null){
                    flowGen.addPacket(basicPacket);
                    nValid++;
                }else{
                    nDiscarded++;
                }
            }catch(PcapClosedException e){
                break;
            }
        }
        long end = System.currentTimeMillis();

        chunks.clear();
        chunks.add(String.format("Done! in %d seconds",((end-start)/1000)));
        chunks.add(String.format("\t Total packets: %d",nTotal));
        chunks.add(String.format("\t Valid packets: %d",nValid));
        chunks.add(String.format("\t Ignored packets:%d %d ", nDiscarded,(nTotal-nValid)));
        chunks.add(String.format("PCAP duration %d seconds",((packetReader.getLastPacket()-packetReader.getFirstPacket())/1000)));
        chunks.add(DividingLine);
        int singleTotal = flowGen.dumpLabeledFlowBasedFeatures(outPath, nameWithoutExt+".csv", FlowFeature.getHeader());
        chunks.add(String.format("Number of Flows: %d",singleTotal));
        chunks.add("");
        publish(chunks.toArray( new String[chunks.size()]));
        totalFlows += singleTotal;
        for (String str : chunks) {
            System.out.println(str);
        }
    }
}
