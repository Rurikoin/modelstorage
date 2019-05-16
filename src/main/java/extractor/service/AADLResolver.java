package extractor.service;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Id;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import extractor.DAO.mapper.InvocationChannelMapper;
import extractor.DAO.mapper.MessageChannelMapper;
import extractor.DAO.mapper._eventMapper;
import extractor.DAO.mapper._exceptionMapper;
import extractor.DAO.mapper._partitionMapper;
import extractor.DAO.mapper._provideMapper;
import extractor.DAO.mapper._requireMapper;
import extractor.DAO.mapper._stateMapper;
import extractor.DAO.mapper._taskMapper;
import extractor.DAO.mapper.busMapper;
import extractor.DAO.mapper.communicationchannelMapper;
import extractor.DAO.mapper.componentMapper;
import extractor.DAO.mapper.deviceMapper;
import extractor.DAO.mapper.linkpointMapper;
import extractor.DAO.mapper.rtosMapper;
import extractor.DAO.mapper.transitionMapper;
import extractor.DAO.mapper.transitionstateMapper;
import extractor.model.ASyncMessaging;
import extractor.model.DispatchChannel;
import extractor.model.InvocationChannel;
import extractor.model.MessageChannel;
import extractor.model._event;
import extractor.model._exception;
import extractor.model._partition;
import extractor.model._provide;
import extractor.model._require;
import extractor.model._state;
import extractor.model._task;
import extractor.model.bus;
import extractor.model.communicationchannel;
import extractor.model.component;
import extractor.model.device;
import extractor.model.linkpoint;
import extractor.model.rtos;
import extractor.model.shareddataaccess;
import extractor.model.syncinterface;
import extractor.model.transition;
import extractor.model.transitionstate;
import javassist.bytecode.stackmap.BasicBlock.Catch;
import net.bytebuddy.asm.Advice.Return;

//解析模型,本模块只负责模型文件的解析
@Service("AADLResolver")
public class AADLResolver {
//	@Autowired
//	private ModelService ms;
	// private ModelService ms;
	// 存储所有模型文件的文件夹
	Map<String, String> aadlFiles = new HashMap<String, String>();
	static String modeldirectory;
	static String dynamicfilename;

	static String hardmodelfile;
	static String errlibfile;
	static String compositelibfile;

	static List<? extends Node> components = null;
	static List<? extends Node> cchannels = null;
	static Map<String, component> componentlist = new HashMap<String, component>();
	static Map<String, component> taskcomponentlist = new HashMap<String, component>();
	static List<_task> tasklist = new ArrayList<_task>();

	static List<linkpoint> portlist = new ArrayList<linkpoint>();
	static List<_require> requirelist = new ArrayList<_require>();
	static List<_provide> providelist = new ArrayList<_provide>();

	static List<_exception> exceptionlist = new ArrayList<_exception>();
	static List<device> devicelist = new ArrayList<device>();
	static List<bus> buslist = new ArrayList<bus>();
	static List<rtos> rtoslist = new ArrayList<rtos>();

	static List<shareddataaccess> sdalist = new ArrayList<shareddataaccess>();
	static List<syncinterface> synclist = new ArrayList<syncinterface>();
	static List<ASyncMessaging> ASMlist = new ArrayList<ASyncMessaging>();

	static List<communicationchannel> cclist = new ArrayList<communicationchannel>();
	static List<InvocationChannel> ivclist = new ArrayList<InvocationChannel>();
	static List<DispatchChannel> dpclist = new ArrayList<DispatchChannel>();
	static List<MessageChannel> mclist = new ArrayList<MessageChannel>();

	static List<_event> eventlist = new ArrayList<_event>();
	static List<_state> statelist = new ArrayList<_state>();

	static List<transition> tslist = new ArrayList<transition>();
	static List<transitionstate> ts_slist = new ArrayList<transitionstate>();

	static List<_partition> partitionlist = new ArrayList<_partition>();

	public static Document ModelResolver(String url) throws DocumentException {
		SAXReader reader = new SAXReader();
		Document document = reader.read(url);
		return document;
	}

//filename参数是hardwareArchitecture

	public void MatchCChannel(String modelfilename, String modelType) throws Exception {
		Document document = ModelResolver(modelfilename);
		List<String> namelist = new ArrayList<>();
		if (modelType.equals("aadl")) {
			String getCChannel = "";
			// hardware层的cchannel解析
			// bus access或者叫asyncmessaging
			String getmessagechannel = "//ownedPublicSection/ownedClassifier[@xsi:type='aadl2:SystemImplementation']/ownedAccessConnection";
			cchannels = document.selectNodes(getmessagechannel);
			ResolveCChannel(modelfilename, "asyncmessaging");
			// invocationChannel
			String getsync = "//ownedPublicSection/ownedClassifier[@xsi:type='aadl2:SystemImplementation']/ownedPortConnection";
			cchannels = document.selectNodes(getsync);
			ResolveCChannel(modelfilename, "sync");
			// dispatchChannel
			// TODO 修改案例完成dispatchChannel的解析xpath
//			String getdc = "//ownedPublicSection/";
//			cchannels = document.selectNodes(getdc);
//			ResolveCChannel(modelfilename, "dispatchChannel"); 
		}
	}

	private void ResolveCChannel(String modelfilename, String t) throws Exception {
		for (Node n : cchannels) {
			Element element = (Element) n;
			communicationchannel cchannel = new communicationchannel();
			Integer idString = (int) GetID.getId();
			cchannel.setName(element.attributeValue("name"));
			cchannel.setCommunicationchannelid(idString);

			switch (t) {
			case "asyncmessaging":
				// TODO 解析dispatch的source,dest
				// TODO 解析inbuffer,outbuffer
				// context是目标元素在本文件中的路径
				// connectionEnd是连了哪个端口
				// source有context是组件向bus发送，没有context是组件接收bus的数据
				if (element.element("source").attribute("context") != null) {
					// inbuffer
					// 解析source
					// 端口会重名但是组件名不会，先找组件名再找组件名下的端口id
					String CompositeName = GetName(modelfilename, element.element("source").attributeValue("context"));
					String PortName = GetName(compositelibfile,
							element.element("source").attributeValue("connectionEnd"));

					Integer sourceportid = getPortIDByComponentName(CompositeName, PortName);
					cchannel.setSourceid(sourceportid);
				} else {
					// outbuffer
					// 此时source是bus
					String busname = GetName(modelfilename, element.element("source").attributeValue("connectionEnd"));
					Integer busid = getCmpIDbyName(busname);
					cchannel.setSourceid(busid);
				}
				// dest没有context是组件向bus发送，有context是组件接收bus的数据
				if (element.element("destination").attribute("context") == null) {
					// inbuffer
					// dest是bus
					String busname = GetName(modelfilename,
							element.element("destination").attributeValue("connectionEnd"));
					Integer busid = getCmpIDbyName(busname);
					cchannel.setDestid(busid);
				} else {
					// outbuffer
					// dest是端口
					String CompositeName = GetName(modelfilename,
							element.element("destination").attributeValue("context"));
//					String PortName = GetName("src/main/resources/modelresource/JH_FK/packages/Composition.aaxl2",
//							element.element("destination").attributeValue("connectionEnd"));
					String PortName = GetName(compositelibfile,
							element.element("destination").attributeValue("connectionEnd"));

					Integer destportid = getPortIDByComponentName(CompositeName, PortName);
					cchannel.setDestid(destportid);
				}
				cchannel.setType("asyncmessaging");
				MessageChannel b = new MessageChannel();
				b.setMessagechannelid(idString);
				mclist.add(b);
				break;
			case "sync":
//				String CompositeName = GetName(modelfilename, element.element("source").attributeValue("context"));
//				String PortName = GetName(compositelibfile, element.element("source").attributeValue("connectionEnd"));
//				Integer sourceportid = getPortIDByComponentName(CompositeName, PortName);
				String CompositeFIleName = modeldirectory 
						+ Getfilename(element.element("source").attributeValue("connectionEnd"));
				String sourceportid = GetElementID(CompositeFIleName,
						element.element("source").attributeValue("connectionEnd"));
				if (sourceportid.equals("")) {
					cchannel.setSourceid(0);
				} else {

					cchannel.setSourceid(Integer.valueOf(sourceportid));
				}

//				String CompositeName1 = GetName(modelfilename,
//						element.element("destination").attributeValue("context"));
				String CompositeName1 = modeldirectory 
						+ Getfilename(element.element("destination").attributeValue("connectionEnd"));
//				String PortName1 = GetName(compositelibfile,
//						element.element("destination").attributeValue("connectionEnd"));
				String destid = GetElementID(CompositeName1,
						element.element("destination").attributeValue("connectionEnd"));
				// Integer destportid = getPortIDByComponentName(CompositeName1, PortName1);
				// TODO 有的没source dest
				if (destid.equals("")) {
					cchannel.setDestid(0);
				} else {
					cchannel.setDestid(Integer.valueOf(destid));
				}

				cchannel.setType("sync");
				InvocationChannel r = new InvocationChannel();
				r.setInvocationchannelid(idString);
				ivclist.add(r);
				break;
			case "dispatchChannel":
				cchannel.setType("dispatchChannel");
				DispatchChannel d = new DispatchChannel();
				d.setDispatchchannelid(idString);
				dpclist.add(d);
				break;
			}
			cclist.add(cchannel);
		}
	}

//解析source路径,传递context，跨文件查找
	private static String GetName(String modelfilename, String rawpath) throws Exception {
		Document document = ModelResolver(modelfilename);
		CharSequence s = ".";
		if (rawpath.contains(s)) {

			rawpath = GetXPath(rawpath);
		}
		Node node = document.selectSingleNode(rawpath);
		Element e = (Element) node;
		if (e != null)
			return e.attributeValue("name");
		else
			return "";
	}

	private static String GetElementID(String modelfilename, String rawpath) throws Exception {
		Document document = ModelResolver(modelfilename);
		CharSequence s = ".";
		if (rawpath.contains(s)) {

			rawpath = GetXPath(rawpath);
		}
		Node node = document.selectSingleNode(rawpath);
		Element e = (Element) node;
		if (e != null)
			return e.attributeValue("id");
		else
			return "";
	}

	/*
	 * 根据路径获取ID,只支持三层结构的xpath
	 */
	private static String GetXPath(String path) {
		String reg = "(?<=@).[A-Za-z0-9\\.]+";
		ArrayList<String> result = new ArrayList<String>();
		Pattern pattern = Pattern.compile(reg);
		Matcher matcher = pattern.matcher(path);
		while (matcher.find()) {
			result.add(matcher.group());
		}
		for (int j = 1; j < result.size(); j++) {

			result.set(j, getinc(result.get(j)));
		}
		return "//" + result.get(0) + "/" + result.get(1) + "/" + result.get(2);
	}

	private static String GetXPath4State(String path) {
		String reg = "(?<=@).[A-Za-z0-9\\.]+";
		ArrayList<String> result = new ArrayList<String>();
		Pattern pattern = Pattern.compile(reg);
		Matcher matcher = pattern.matcher(path);
		while (matcher.find()) {
			result.add(matcher.group());
		}
		for (int j = 1; j < result.size(); j++) {

			result.set(j, getinc(result.get(j)));
		}
		return "//" + result.get(0) + "/" + result.get(1) + "/" + result.get(2) + "/" + result.get(3) + "/"
				+ result.get(4);
	}

	private static String getinc(String source) {
		CharSequence cs = ".";
		if (source.contains(cs)) {
			String reg4c = "[A-Za-z]+";
			String reg4num = "[0-9]+";

			Pattern pattern = Pattern.compile(reg4c);
			Matcher matcher = pattern.matcher(source);
			List<String> list = new ArrayList<>();
			while (matcher.find()) {
				list.add(matcher.group());
			}
			String c = list.get(0);

			pattern = Pattern.compile(reg4num);
			matcher = pattern.matcher(source);
			list = new ArrayList<>();
			while (matcher.find()) {
				list.add(matcher.group());
			}
			Integer i = Integer.valueOf(list.get(0));
			String num = (++i).toString();

			return c + "[" + num + "]";
		} else {
			return source;
		}

	}

	public void initComponentID(String filepath) throws Exception {
		String getbus = "//ownedClassifier[@xsi:type='aadl2:SystemImplementation']/ownedBusSubcomponent";
		String getdevice = "//ownedClassifier[@xsi:type='aadl2:SystemImplementation']/ownedDeviceSubcomponent";
		String getsys = "//ownedClassifier[@xsi:type='aadl2:SystemImplementation']/ownedSystemSubcomponent";
		Document document = ModelResolver(filepath);
		List<? extends Node> nodes = document.selectNodes(getbus);
		for (Node n : nodes) {
			Integer idString = (int) GetID.getId();
			AppendID.AppendID(filepath, n.getUniquePath(), idString.toString());
		}
		nodes = document.selectNodes(getsys);
		nodes.forEach((v) -> {
			Integer idString2 = (int) GetID.getId();
			try {
				AppendID.AppendID(filepath, v.getUniquePath(), idString2.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		nodes = document.selectNodes(getdevice);
		for (Node n : nodes) {
			Integer idString3 = (int) GetID.getId();
			AppendID.AppendID(filepath, n.getUniquePath(), idString3.toString());
		}
	}

	/* 当前模型的全部组件与port、task */
	public void MatchComponents(String filepath, String modelType, String contenttype) throws Exception {
		Document document = ModelResolver(filepath);

		List<String> namelist = new ArrayList<>();
		if (modelType.equals("aadl")) {
			// 对hardware层级设计的三种与组件元素的解析
			String getbus = "//ownedClassifier[@xsi:type='aadl2:SystemImplementation']/ownedBusSubcomponent";
			String getsys = "//ownedClassifier[@xsi:type='aadl2:SystemImplementation']/ownedSystemSubcomponent";
			String getdevice = "//ownedClassifier[@xsi:type='aadl2:SystemImplementation']/ownedDeviceSubcomponent";

			// Integer id1 = (int) GetID.getId();
			components = document.selectNodes(getbus);
			ResolveComponents(filepath, "bus");

			// Integer id2 = (int) GetID.getId();
			components = document.selectNodes(getsys);
			ResolveComponents(filepath, "sys");

			components = document.selectNodes(getdevice);
			ResolveComponents(filepath, "device");

		} else if (modelType.equals("sysml")) {
			components = document
					.selectNodes("//packagedElement[@name='kf']/ownedOperation[@xmi:type='uml:Operation']");
			for (Node n : components) {
				Element element = (Element) n;
				namelist.add(element.attributeValue("name"));
			}
		} else {
			components = document.selectNodes("//ModelInformation/Model/System/System/Block");
			for (Node n : components) {
				Element element = (Element) n;
				namelist.add(element.attributeValue("name"));
			}
		}
	}

	// TODO rtos与processor的关系绑定
	public void InnerSystem(String modelfilename) throws Exception {
		Document document = ModelResolver(modelfilename);

		// 从集成的角度看，先直接解析task，等以后有了shcedule再绑定上级
		String gettask = "//ownedClassifier[@xsi:type='aadl2:ProcessType' or @xsi:type='aadl2:ThreadType']";
		// 查找系统内部的组件
		// String innerDevcvice = "//ownedClassifier[@xsi:type='aadl2:ProcessorType'or
		// @xsi:type='aadl2:MemoryType']";
		String innerDevcvice = "//ownedClassifier[@xsi:type='aadl2:ProcessorType']";
		// Integer idString3 = (int) GetID.getId();
		components = document.selectNodes(innerDevcvice);
		resolverChild(modelfilename, "device");

		components = document.selectNodes(gettask);
		// TODO 部署关系的解析
		TaskResolver(modelfilename);

		// SystemType的错误附件影响到下级的实现
		// String getfathersyString = "";

		String getallsysimpl = "//ownedClassifier[@xsi:type='aadl2:SystemImplementation']";
		List<? extends Node> nodes = document.selectNodes(getallsysimpl);
		for (Node n : nodes) {
			Element e = (Element) n;
			StateResolver(modelfilename, e.attributeValue("id"));
		}
	}

	// 内部的deivce与上级元素都需要放进component表中
	private void resolverChild(String filepath, String t) throws Exception {
		List<? extends Node> ports = null;
		for (Node n : components) {
			Element element = (Element) n;
			Integer idString2 = (int) GetID.getId();

			AppendID.AppendID(filepath, n.getUniquePath(), idString2.toString());

			component component = new component();
			component.setModeltype("aadl");
			component.setName(element.attributeValue("name"));
			component.setComponentid(idString2);
			component.setType("partition");

			insert_component(component);
			// partition
//			String a=element.attributeValue("xsi:type");
//			if (element.attributeValue("xsi:type").equals("aadl2:ProcessorType")) {
			// _partition partition = new _partition();
			// partition.setPartitionid(idString2);
			// partition.setRtosid();
			// partitionlist.add(partition);
			// }
			// TODO RTOS与partition的部署关系
//			device d = new device();
//			d.setDeviceid(idString2);
//			devicelist.add(d);
			// 名下的linkpoint
			String devicedef = modeldirectory + Getfilename(element.attributeValue("deviceSubcomponentType"));
			LinkpointResolver(devicedef, ports, idString2, t, element.attributeValue("name"));

			// componentlist.put(element.attributeValue("name"), component);
		}
	}

	private static void ResolveComponents(String filepath, String t) throws Exception {
		List<? extends Node> ports = null;
		for (Node n : components) {
			Element element = (Element) n;
			component component = new component();

			Integer componentID = Integer.valueOf(element.attributeValue("id"));
			// AppendID.AppendID(filepath, element.getPath(), idString.toString());

			component.setModeltype("aadl");
			component.setName(element.attributeValue("name"));
			component.setComponentid(componentID);
			switch (t) {
			case "bus":
				component.setType("bus");
				bus b = new bus();
				b.setBusid(componentID);
				buslist.add(b);
				break;
			case "sys":
				dynamicfilename = Getfilename(element.attributeValue("systemSubcomponentType"));
				component.setType("rtos");
				rtos r = new rtos();
				r.setRtosid(componentID);
				// TODO 设置partitions的数量
				rtoslist.add(r);

				String sysdef = modeldirectory + Getfilename(element.attributeValue("systemSubcomponentType"));
				LinkpointResolver(sysdef, ports, componentID, t, element.attributeValue("name"));
				break;
			case "device":
				component.setType("device");
				device d = new device();
				d.setDeviceid(componentID);
				devicelist.add(d);
				// 名下的linkpoint
				// String devicedef = modeldirectory +
				// Getfilename(element.attributeValue("deviceSubcomponentType"));
				LinkpointResolver(compositelibfile, ports, componentID, t, element.attributeValue("name"));
				break;
			}
			componentlist.put(element.attributeValue("name"), component);
		}
	}

//TODO dataobject
	private static void LinkpointResolver(String linkpointfile, List<? extends Node> ports, Integer fatherid,
			String fathertype, String componetname) throws Exception {
		switch (fathertype) {
		case "bus":
		case "device":
			Document document = ModelResolver(compositelibfile);
			// 构建linkpoint,分开处理busaccess与dataport,eventport,busaccess看require属性,dataport与之前相同
			// bussaccess
			ports = document
					.selectNodes("//ownedPublicSection/ownedClassifier[@name='" + componetname + "']/ownedBusAccess");
			TraverseOwnedPorts(ports, fatherid, "busaccess");
			// dataport
			ports = document
					.selectNodes("//ownedPublicSection/ownedClassifier[@name='" + componetname + "']/ownedDataPort");
			TraverseOwnedPorts(ports, fatherid, "dataport");
			// eventport
			ports = document
					.selectNodes("//ownedPublicSection/ownedClassifier[@name='" + componetname + "']/ownedEventPort");
			// 对应关系，busaccess对应----- eventport对应------ dataport对应--------
			TraverseOwnedPorts(ports, fatherid, "eventport");
			break;
		case "sys":
			Document document2 = ModelResolver(linkpointfile);
			dynamicfilename = linkpointfile;
			// 同上
			ports = document2.selectNodes("//ownedClassifier[@xsi:type='aadl2:SystemType']/ownedDataPort");
			TraverseOwnedPorts4Sys(ports, fatherid, "dataport");
			ports = document2.selectNodes("//ownedClassifier[@xsi:type='aadl2:SystemType']/ownedEventPort");
			TraverseOwnedPorts4Sys(ports, fatherid, "eventport");
			ports = document2.selectNodes("//ownedClassifier[@xsi:type='aadl2:SystemType']/ownedBusAccess");
			TraverseOwnedPorts4Sys(ports, fatherid, "busaccess");
			break;
		}
	}

	private static void TraverseOwnedPorts4Sys(List<? extends Node> ports, Integer fatherid, String portType)
			throws Exception {
		for (Node n2 : ports) {
			Element element2 = (Element) n2;

			linkpoint ports1 = new linkpoint();
			// 暂时只设置name这一个
			ports1.setName(element2.attributeValue("name"));
			Integer linkpointID = (int) GetID.getId();
			switch (portType) {
			case "dataport":
			case "eventport":
				syncinterface si = new syncinterface();
				si.setSyncinterfaceid(linkpointID);
				try {
					AppendID.AppendID(dynamicfilename, element2.getUniquePath(), linkpointID.toString());
				} catch (Exception e) {
					e.printStackTrace();
				}

				if (!(element2.attribute("in") == null)) {
					// 是输入端口
					_require r = new _require();
					r.setRequired(linkpointID);
					r.setRequirer(fatherid);
					requirelist.add(r);
				} else {
					_provide p = new _provide();
					p.setProvided(linkpointID);
					p.setProvider(fatherid);
					providelist.add(p);
				}
				break;
			case "busaccess":
				AppendID.AppendID(dynamicfilename, element2.getUniquePath(), linkpointID.toString());
				ASyncMessaging asm = new ASyncMessaging();
				asm.setAsyncmessagingid(linkpointID);
				if (element2.attribute("kind").equals("requires")) {
					// 是输入端口
					_require r = new _require();
					r.setRequired(linkpointID);
					r.setRequirer(fatherid);
					requirelist.add(r);
				} else {
					_provide p = new _provide();
					p.setProvided(linkpointID);
					p.setProvider(fatherid);
					providelist.add(p);
				}
				break;
			}
			ports1.setLinkpointid(linkpointID);
			portlist.add(ports1);
		}
	}

	private static void TraverseOwnedPorts(List<? extends Node> ports, Integer fatherid, String portType)
			throws Exception {
		for (Node n2 : ports) {
			Element element2 = (Element) n2;

			linkpoint ports1 = new linkpoint();
			// 暂时只设置name这一个
			ports1.setName(element2.attributeValue("name"));
			Integer linkpointID = (int) GetID.getId();
			// TODO taskschedule的创建入库
			switch (portType) {
			case "dataport":
			case "eventport":
				syncinterface si = new syncinterface();
				si.setSyncinterfaceid(linkpointID);
				try {

					AppendID.AppendID(compositelibfile, element2.getUniquePath(), linkpointID.toString());

				} catch (Exception e) {
					e.printStackTrace();
				}

				if (!(element2.attribute("in") == null)) {
					// 是输入端口
					_require r = new _require();
					r.setRequired(linkpointID);
					r.setRequirer(fatherid);
					requirelist.add(r);
				} else {
					_provide p = new _provide();
					p.setProvided(linkpointID);
					p.setProvider(fatherid);
					providelist.add(p);
				}
				break;
			case "busaccess":
				AppendID.AppendID(compositelibfile, element2.getUniquePath(), linkpointID.toString());
				ASyncMessaging asm = new ASyncMessaging();
				asm.setAsyncmessagingid(linkpointID);
				if (element2.attribute("kind").equals("requires")) {
					// 是输入端口
					_require r = new _require();
					r.setRequired(linkpointID);
					r.setRequirer(fatherid);
					requirelist.add(r);
				} else {
					_provide p = new _provide();
					p.setProvided(linkpointID);
					p.setProvider(fatherid);
					providelist.add(p);
				}
				break;
			}
			ports1.setLinkpointid(linkpointID);
			portlist.add(ports1);
		}
	}

	// 错误附件的解析
	public void ExceptionResolver(String modelfilename, String modelType) throws Exception {
		Document document = ModelResolver(modelfilename);

		List<? extends Node> nodelist = new ArrayList<>();
		if (modelType.equals("aadl")) {
			// 读取system节点
			nodelist = document.selectNodes("//ownedClassifier[@xsi:type='aadl2:SystemType']");
			nodelist.forEach((v) -> {

				Element element = (Element) v;
				String compositename = element.attributeValue("name");

				_exception e = new _exception();
				e.setName(element.element("ownedAbstractFeature").attributeValue("name"));
				String exceptionTypeString = getType(
						element.element("ownedAnnexSubclause").element("parsedAnnexSubclause").element("propagations")
								.element("typeSet").element("typeTokens").attributeValue("type"));
				e.setType(exceptionTypeString);
				element = element.element("ownedAnnexSubclause").element("parsedAnnexSubclause")
						.element("propagations");
				Integer sourceid = 0, destid = 0;
				// 没有direction或者
				if (element.attributeValue("direction").equals("out")) {
					String getsourceportPath = element.element("featureorPPRef").attributeValue("featureorPP");
					try {
						sourceid = getPortIDByComponentName(compositename,
								GetName(modelfilename, GetXPath(getsourceportPath)));
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				} else {
					String getsourceportPath = element.element("featureorPPRef").attributeValue("featureorPP");
					try {
						destid = getPortIDByComponentName(compositename,
								GetName(modelfilename, GetXPath(getsourceportPath)));
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
				e.setCommunicationchannelid(getCChannelBysd(sourceid, destid));
				exceptionlist.add(e);
			});

		}
	}

	// 需要的文件是系统内部结构，而且要先解析错误库中的event
	public void TransitionResolver(String modelfilename, String modelType) throws Exception {
		Document document = ModelResolver(modelfilename);
		List<? extends Node> transNodes = document
				.selectNodes("//ownedAnnexSubclause/parsedAnnexSubclause/transitions");

		transNodes.forEach((v) -> {
			Element element = (Element) v;
			// transition与作为trigger的event关联
			String getTriggerevent = element.element("condition").element("qualifiedErrorPropagationReference")
					.element("emv2Target").attributeValue("namedElement");

			transition t = new transition();
			try {
				t.setTransitionid((int) GetID.getId());
				// 关联event
				Document d = ModelResolver(errlibfile);
				Node nodes = d.selectSingleNode(GetXPath4State(getTriggerevent));
//				for(Node n:nodes) {
				String id = ((Element) nodes).attributeValue("id");
				t.setTrigger(Integer.valueOf(id));
				t.setName(getType(element.attributeValue("name")));
				transitionstate tsTransitionstate = new transitionstate();
				tslist.add(t);
//				}
				// e2.setTrigger(getEventID(GetName(modelfilename,
				// GetXPath4State(getTriggerevent))));

				tsTransitionstate.setSource(
						getStateID(GetName(dynamicfilename, GetXPath4State(element.attributeValue("source")))));
				tsTransitionstate
						.setOut(getStateID(GetName(dynamicfilename, GetXPath4State(element.attributeValue("target")))));

				tsTransitionstate.setTransitionid(t.getTransitionid());
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	// 系统内部结构
	public static void eventResolver(String modelfilename, String modelType) throws Exception {
		// TODO 下一个event的确定
		// 外部的event
		Document d = ModelResolver(errlibfile);
		List<? extends Node> outevent = d
				.selectNodes("//ownedPublicSection/ownedAnnexLibrary/parsedAnnexLibrary/behaviors/events");
		for (Node n : outevent) {
			Element element = (Element) n;
			_event e2 = new _event();
			e2.setName(element.attributeValue("name"));
			Integer idString = (int) GetID.getId();
			e2.setEventid(idString);
			AppendID.AppendID(errlibfile, n.getUniquePath(), idString.toString());
			eventlist.add(e2);
		}
		// 解析错内部的event
		Document document = ModelResolver(modelfilename);
		List<? extends Node> erroreventName = document.selectNodes("//ownedAnnexSubclause/parsedAnnexSubclause/events");
		erroreventName.forEach((v) -> {
			Element element = (Element) v;
			_event e2 = new _event();
			e2.setName(element.attributeValue("name"));
			Integer idString = (int) GetID.getId();
			try {
				AppendID.AppendID(modelfilename, v.getUniquePath(), idString.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
			eventlist.add(e2);
		});

	}

	// 需要系统内部结构文件,state是组件的state
	private void StateResolver(String modelfilename, String Compositeid) throws Exception {
		// TODO state与interface的关联
		Document document = ModelResolver(modelfilename);

		List<? extends Node> stateInfo = document
				.selectNodes("//ownedAnnexSubclause/parsedAnnexSubclause/states/condition/operands/operands");
		if (stateInfo.size() != 0) {
			// 文件中定义了state
			stateInfo.forEach((v) -> {
				Element element = (Element) v;
				try {
					AppendID.AppendID(modelfilename, v.getUniquePath(), Compositeid);
				} catch (Exception e2) {
					e2.printStackTrace();
				}
				String statenameString = element.element("qualifiedState").attributeValue("state");

				String cmpID = element.element("qualifiedState").element("subcomponent").attributeValue("subcomponent");
				_state s = new _state();
				try {
					// s.setComponentid(getCmpIDbyName(GetName(hardmodelfile, GetXPath(cmpID))));
					s.setComponentid(Integer.valueOf(Compositeid));
					s.setName(GetName(errlibfile, GetXPath4State(statenameString)));
				} catch (Exception e) {
				}
				statelist.add(s);
			});
		}
		// 5.10添加解析规则：如果引用了behavior则错误库中该behavior下的state一并引入进来
		Node behaviorInfo = document.selectSingleNode("//ownedAnnexSubclause/parsedAnnexSubclause");
		if (behaviorInfo != null) {
			Element e1 = (Element) behaviorInfo;
			try {
				String s = "//ownedAnnexLibrary/parsedAnnexLibrary/behaviors[@name='"
						+ getType(e1.attributeValue("useBehavior")) + "']/states";
				List<? extends Node> libstateinfo = ModelResolver(errlibfile).selectNodes(s);
				libstateinfo.forEach((v) -> {
					_state stat = new _state();
					stat.setComponentid(Integer.valueOf(Compositeid));
					stat.setName(((Element) v).attributeValue("name"));
					statelist.add(stat);
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	// 获取Exception的类型
	private static String getType(String typepath) {
		if (typepath.contains(".")) {

			String[] s = typepath.split("\\.");
			return s[s.length - 1];
		}
		return "";
	}

	private static void TaskResolver(String modelfilename) throws Exception {
		// Document document = ModelResolver(modelfilename);

		for (Node n : components) {
			// 解析component
			Element element = (Element) n;

			component component = new component();
			Integer idString = (int) GetID.getId();

			AppendID.AppendID(modelfilename, n.getUniquePath(), idString.toString());

			component.setComponentid(idString);

			component.setModeltype("aadl");
			component.setName(element.attributeValue("name"));
			component.setType("task");
			// TODO task作为component的存入
			componentlist.put("", component);
			// 解析task
			_task t = new _task();
			t.setName(component.getName());
			t.setTaskid(idString);
			// TODO 解析遍历partition
			// t.setPartitionid(partitionlist.get(0).getPartitionid());
			tasklist.add(t);
			taskcomponentlist.put(element.attributeValue("name"), component);
		}
	}

	private static String Getfilename(String systemSubcomponentType) {
		if (systemSubcomponentType != null) {
			String reg = "(?<=\\s)[A-Za-z0-9]+";
			Pattern pattern = Pattern.compile(reg);
			Matcher matcher = pattern.matcher(systemSubcomponentType);
			ArrayList<String> result = new ArrayList<String>();
			while (matcher.find()) {
				result.add(matcher.group());
			}
			if (result.size() > 0) {
				return result.get(0) + ".aaxl2";
			}
		}
		return "";
	}

	private static String GetSysname(String systemSubcomponentType) {
		if (systemSubcomponentType != null) {
			String reg = "(?<=#).*\\.[A-Za-z0-9]+";
			Pattern pattern = Pattern.compile(reg);
			Matcher matcher = pattern.matcher(systemSubcomponentType);
			ArrayList<String> result = new ArrayList<String>();
			while (matcher.find()) {
				result.add(matcher.group());
			}
			if (result.size() > 0) {
				String[] strings = result.get(0).split("\\.");
				return strings[strings.length - 2] + "." + strings[strings.length - 1];
			}
		}
		return "";
	}

	@Autowired
	private componentMapper camArchMapper;
	@Autowired
	private linkpointMapper portsMapper;
	@Autowired
	private _provideMapper pM;
	@Autowired
	private _requireMapper rm;
	@Autowired
	private transitionMapper tm;
	@Autowired
	private transitionstateMapper tsm;
	@Autowired
	private _eventMapper em;
	@Autowired
	private _stateMapper sm;
	@Autowired
	private deviceMapper dMapper;
	@Autowired
	private busMapper bm;
	@Autowired
	private communicationchannelMapper cchannelMapper;
	@Autowired
	private MessageChannelMapper mcm;
	@Autowired
	private InvocationChannelMapper ivcm;
	@Autowired
	private _exceptionMapper _em;
	@Autowired
	private rtosMapper rsmMapper;
	@Autowired
	private _taskMapper _tm;
	@Autowired
	private _partitionMapper ptm;

	private int insert_partition(_partition p) {
		return ptm.insert(p);
	}

	private int insert_component(component c) {
		return camArchMapper.insert(c);
	}

	private int insert_bus(bus b) {
		return bm.insert(b);
	}

	private int insert_ports(linkpoint p) {
		return portsMapper.insert(p);
	}

	private int insert_require(_require r) {
		return rm.insert(r);
	}

	private int insert_provide(_provide p) {
		return pM.insert(p);
	}

	private int insert_cchannel(communicationchannel c) {
		return cchannelMapper.insert(c);
	}

	private int insert_mchannel(MessageChannel m) {
		return mcm.insert(m);
	}

	private int insert_ivcchannel(InvocationChannel i) {
		return ivcm.insert(i);
	}

	private int insert_device(device d) {
		return dMapper.insert(d);
	}

	private int insert_task(_task t) {
		return _tm.insert(t);
	}

	private int insert_transition(transition t) {
		return tm.insert(t);
	}

	private int insert_tss(transitionstate ts) {
		return tsm.insert(ts);
	}

	private int insert_rtos(rtos r) {
		return rsmMapper.insert(r);
	}

	private int insert_state(_state s) {
		return sm.insert(s);
	}

	private int insert_event(_event e) {
		return em.insert(e);
	}

	private int insert_exception(_exception e) {
		return _em.insert(e);
	}

	public Integer getPortIDByComponentName(String name, String portname) {
		Integer aIntegers = camArchMapper.getPortIDByComponentName(name, portname);
		return camArchMapper.getPortIDByComponentName(name, portname);
	}

	public Integer getCmpIDbyName(String name) {
		return camArchMapper.getIDbyName(name).getComponentid();
	}

	public Integer getCChannelBysd(Integer sid, Integer did) {
		return cchannelMapper.getgetCChannelBysd(sid, did);
	}

	public Integer getEventID(String eventname) {
		return em.getEventID(eventname).getEventid();
	}

	public Integer getStateID(String statename) {
		return sm.getStateID(statename);
	}

	public void setAadlFiles(Map<String, String> aadlFiles) {
		this.aadlFiles = aadlFiles;
	}

	// 模型元素与元模型进行匹配
	public void srvmatchmeta() throws Exception {
		AADLResolver.modeldirectory = "src/main/resources/modelresource/MarkedModelFile/";

		AADLResolver.compositelibfile = aadlFiles.get("组件库");
		AADLResolver.errlibfile = aadlFiles.get("错误库");
		AADLResolver.hardmodelfile = aadlFiles.get("总体架构");
		MatchComponents(hardmodelfile, "aadl", "总体架构");
		AADLResolver.componentlist.forEach((k, v) -> {
			insert_component(v);
		});

		rtoslist.forEach((v) -> {
			insert_rtos(v);
		});
		buslist.forEach((v) -> {
			insert_bus(v);
		});
		devicelist.forEach((v) -> {
			insert_device(v);
		});
		portlist.forEach((v) -> {
			insert_ports(v);
		});
		requirelist.forEach((v) -> {
			insert_require(v);
		});
		providelist.forEach((v) -> {
			insert_provide(v);
		});
		MatchCChannel(hardmodelfile, "aadl");
		cclist.forEach((v) -> {
			insert_cchannel(v);
		});
		mclist.forEach((v) -> {
			insert_mchannel(v);
		});
		ivclist.forEach((v) -> {
			insert_ivcchannel(v);
		});
		String[] innersysfile = aadlFiles.get("系统内部结构").split(";");
		for (String s : innersysfile) {
			AADLResolver.dynamicfilename = s;

			InnerSystem(dynamicfilename);
		}

		partitionlist.forEach((v) -> {
			insert_partition(v);
		});
		if (errlibfile != null) {
			ExceptionResolver(dynamicfilename, "aadl");
			exceptionlist.forEach((v) -> {
				insert_exception(v);
			});
		}

		Document d = ModelResolver(aadlFiles.get("系统内部结构"));
		List<? extends Node> nodes = d.selectNodes("//ownedClassifier[@xsi:type='aadl2:SystemImplementation']");
		for (Node n : nodes) {
			StateResolver(aadlFiles.get("系统内部结构"), ((Element) n).attributeValue("id"));
		}
		statelist.forEach((v) -> {
			insert_state(v);
		});
		if (errlibfile != null) {

			eventResolver(aadlFiles.get("系统内部结构"), "aadl");
		}
		eventlist.forEach((v) -> {
			insert_event(v);
		});
		TransitionResolver(aadlFiles.get("系统内部结构"), "aadl");
		tslist.forEach((v) -> {
			insert_transition(v);
		});
		ts_slist.forEach((v) -> {
			insert_tss(v);
		});
		taskcomponentlist.forEach((k, v) -> {
			insert_component(v);
		});
		tasklist.forEach((v) -> {
			insert_task(v);
		});

	}

	public void SetSysFileID(String archfilepath, String sysfilepath) throws Exception {
		Document document = ModelResolver(archfilepath);
		String getsys = "//ownedPublicSection/ownedClassifier[@xsi:type='aadl2:SystemImplementation']/ownedSystemSubcomponent";
		List<? extends Node> nodes = document.selectNodes(getsys);

		for (Node n : nodes) {
			String raw = GetSysname(((Element) n).attributeValue("systemSubcomponentType"));
			String sysname = raw;

			String sys = "//ownedClassifier[@xsi:type='aadl2:SystemImplementation' and @name='" + sysname + "']";
			document = ModelResolver(sysfilepath);
			Element e = (Element) n;

			AppendID.AppendID(sysfilepath, sys, e.attributeValue("id"));
		}
	}
}