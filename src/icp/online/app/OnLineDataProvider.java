package icp.online.app;

import icp.online.tcpip.DataTokenizer;
import icp.online.tcpip.TCPIPClient;
import icp.online.tcpip.objects.RDA_Marker;
import icp.online.tcpip.objects.RDA_MessageData;

public class OnLineDataProvider {
	
	private float[][][] epochs;
	
	private static final int DELKABUFFERU = 10000;
	private static final int POCETHODNOTPREDEPOCHOU = 100;
	private static final int POCETHODNOTZAEPOCHOU = 1000;
	private static final int SHODA = 400000;
	private static final int ZAKAZANYEXTREM = 350;

	/**
	 *  Počet stimulů, po jakém se zastaví hlavní test.
	 */
	private static final int POCETSTIMULU = 150;

	/**
	 * Pole, ve kterém budou uloženy průměrné hodnoty EEG signálu pro jednotlivé čislice
	 * index pole označuje dané čislo (0-9)
	 */
	private Epocha[] epochaCisla;

	/**
	 * Zapisovač pro ukládání počtu podobností jednotlivých (zprůměrovaných) epoch s P-300 vlnou.
	 */
	//private Zapisovac zapisovac;


	/**
	 * Bufer, který se bude používat pro ukládání hodnot z datových objektů RDA_MessageData
	 */
	private Buffer buffer;

	/**
	 * Konstruktor, který vytvoří instanci této řídící třídy.
	 * @param ip_adr - IP adresa serveru, na který se napojí client
	 * @param port - port, který se použije pro komunikaci se serverem
	 * @param log - loger pro logování událostí
	 * @param vystup - přijaté číslo (ze stimulu)
	 * @param reakce reference na zapisovač reakcí
	 * @param p300 - 'idealni' P300 vlna
	 * Tato vlna se nastaví pomocí O-Q testu, a náísledně se používá k porovnávání s
	 * průměrnými epochami číslic
	 */
	public OnLineDataProvider(String ip_adr, int port){
		TCPIPClient client = new TCPIPClient(ip_adr, port);
		client.start();
		DataTokenizer dtk = new DataTokenizer(client);
		dtk.start();

		this.epochaCisla = new Epocha[10];

		/* delku bufferu je nutno zvolit libovolne vhodne */
		this.buffer = new Buffer(DELKABUFFERU,POCETHODNOTPREDEPOCHOU,POCETHODNOTZAEPOCHOU);

		int cisloStimulu = 0;
		while(cisloStimulu < POCETSTIMULU + 1){
			Object o = dtk.retrieveDataBlock();
			if(o instanceof RDA_Marker){
				/* takto získám příchozí číslo z markeru */
				int cislo = ((Integer.parseInt(((RDA_Marker) o).getsTypeDesc().substring(11,13).trim()))-1);
				vystup.setText("" + cislo);
				cisloStimulu++;
			}
			if(o instanceof RDA_MessageData){
				buffer.zapis((RDA_MessageData) o);

			}
			if(buffer.jePlny() || (cisloStimulu > POCETSTIMULU)){
				for(HodnotyVlny data = buffer.vyber(); data != null; data = buffer.vyber()){
					Epocha epocha = new Epocha(POCETHODNOTPREDEPOCHOU,POCETHODNOTZAEPOCHOU,data.getHodnoty(),log);
					if(!epocha.existujeArtefakt(ZAKAZANYEXTREM)){
						if(this.epochaCisla[data.getTypStimulu()] != null){
							this.epochaCisla[data.getTypStimulu()].zprumeruj(epocha);
						}else{
							this.epochaCisla[data.getTypStimulu()] = epocha;
						}
					}
				}
				buffer.vymaz();
				/*for(int i = 0; i < this.epochaCisla.length; i++){
					if(this.epochaCisla[i] != null){
						double shodujeSeNa = this.epochaCisla[i].porovnej(this.p300, 0, POCETHODNOTZAEPOCHOU);
						if(shodujeSeNa > SHODA){
							this.zapisovac.zaznamenejReakci(i);
						}
					}
				}*/
			}
		}
		//int nejvetsiReakce = this.zapisovac.getNejvetsiReakci();

		//vystup.setText("" + nejvetsiReakce);
		//log.log("EXPERIMENT SKONČIL, můžete ukončit měření");
		//log.log("Nejsilnější reakce byla zaznamenána na číslo " + nejvetsiReakce);
	}
	
	/**
	 * @param stimul číslo stimulu - 1 pro číslo jedna, 2 pro číslo dva, atd. až 9
	 * @return dostupné epochy pro všechny dostupné kanály. [index_kanálu][index_epochy][index_vzorku_epochy]
	 */
	public float[][][] getEpochs(int stimul)
	{
		
	}
}