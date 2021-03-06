package luckyclient.caserun.exinterface;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.WebDriver;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.IOSElement;
import luckyclient.caserun.exappium.AppDriverAnalyticCase;
import luckyclient.caserun.exappium.androidex.AndroidCaseExecution;
import luckyclient.caserun.exappium.iosex.IosCaseExecution;
import luckyclient.caserun.exinterface.analyticsteps.InterfaceAnalyticCase;
import luckyclient.caserun.exwebdriver.ex.WebCaseExecution;
import luckyclient.caserun.exwebdriver.ex.WebDriverAnalyticCase;
import luckyclient.caserun.publicdispose.ActionManageForSteps;
import luckyclient.caserun.publicdispose.ChangString;
import luckyclient.dblog.DbLink;
import luckyclient.dblog.LogOperation;
import luckyclient.planapi.api.GetServerAPI;
import luckyclient.planapi.entity.ProjectCase;
import luckyclient.planapi.entity.ProjectCasesteps;
import luckyclient.planapi.entity.PublicCaseParams;
import luckyclient.publicclass.InvokeMethod;
import luckyclient.publicclass.LogUtil;

/**
 * =================================================================
 * 这是一个受限制的自由软件！您不能在任何未经允许的前提下对程序代码进行修改和用于商业用途；也不允许对程序代码修改后以任何形式任何目的的再发布。
 * 为了尊重作者的劳动成果，LuckyFrame关键版权信息严禁篡改
 * 有任何疑问欢迎联系作者讨论。 QQ:1573584944  seagull1985
 * =================================================================
 *
 * @author： seagull
 * @date 2018年3月1日
 */
public class TestCaseExecution {
    protected static final String ASSIGNMENT_SIGN = "$=";
    protected static final String FUZZY_MATCHING_SIGN = "%=";
    protected static final String REGULAR_MATCHING_SIGN = "~=";
    private static Map<String, String> VARIABLE = new HashMap<String, String>(0);
    
    /**
     * @param projectname        项目名
     * @param testCaseExternalId 用例编号
     * @param version            用例版本号
     *                           用于单条用例调试，并通过日志框架写日志到UTP上，用做UTP上单条用例运行
     */
    public static void oneCaseExecuteForTast(String projectname, String testCaseExternalId, int version, String taskid) {
        TestControl.TASKID = taskid;
        DbLink.exetype = 0;
        // 初始化写用例结果以及日志模块
        LogOperation caselog = new LogOperation();
        String packagename = null;
        String functionname = null;
        String expectedresults = null;
        Integer setcaseresult = 0;
        Object[] getParameterValues = null;
        String testnote = "初始化测试结果";
        int k = 0;
        // 删除旧的日志
        LogOperation.deleteCaseLogDetail(testCaseExternalId, taskid);
        ProjectCase testcaseob = GetServerAPI.cgetCaseBysign(testCaseExternalId);
        List<PublicCaseParams> pcplist = GetServerAPI.cgetParamsByProjectid(String.valueOf(testcaseob.getProjectid()));
        // 把公共参数加入到MAP中
        for (PublicCaseParams pcp : pcplist) {
        	VARIABLE.put(pcp.getParamsname(), pcp.getParamsvalue());
        }
        List<ProjectCasesteps> steps = GetServerAPI.getStepsbycaseid(testcaseob.getId());
        if (steps.size() == 0) {
            setcaseresult = 2;
            luckyclient.publicclass.LogUtil.APP.error("用例中未找到步骤，请检查！");
            caselog.caseLogDetail(taskid, testCaseExternalId, "用例中未找到步骤，请检查！", "error", "1", "");
            testnote = "用例中未找到步骤，请检查！";
        }
        // 进入循环，解析用例所有步骤
        for (int i = 0; i < steps.size(); i++) {
            Map<String, String> casescript = InterfaceAnalyticCase.analyticCaseStep(testcaseob, steps.get(i), taskid, caselog);
            try {
                packagename = casescript.get("PackageName");
                packagename = ChangString.changparams(packagename, VARIABLE, "包路径");
                functionname = casescript.get("FunctionName");
                functionname = ChangString.changparams(functionname, VARIABLE, "方法名");
            } catch (Exception e) {
                k = 0;
                luckyclient.publicclass.LogUtil.APP.error("用例：" + testcaseob.getSign() + "解析包名或是方法名失败，请检查！");
                caselog.caseLogDetail(taskid, testcaseob.getSign(), "解析包名或是方法名失败，请检查！", "error", String.valueOf(i + 1), "");
                e.printStackTrace();
                break; // 某一步骤失败后，此条用例置为失败退出
            }
            // 用例名称解析出现异常或是单个步骤参数解析异常
            if ((null != functionname && functionname.contains("解析异常")) || k == 1) {
                k = 0;
                testnote = "用例第" + (i + 1) + "步解析出错啦！";
                break;
            }
            expectedresults = casescript.get("ExpectedResults");
            expectedresults = ChangString.changparams(expectedresults, VARIABLE, "预期结果");
            // 判断方法是否带参数
            if (casescript.size() > 4) {
                // 获取传入参数，放入对象中，初始化参数对象个数
                getParameterValues = new Object[casescript.size() - 4];
                for (int j = 0; j < casescript.size() - 4; j++) {
                    if (casescript.get("FunctionParams" + (j + 1)) == null) {
                        k = 1;
                        break;
                    }

                    String parameterValues = casescript.get("FunctionParams" + (j + 1));
                    parameterValues = ChangString.changparams(parameterValues, VARIABLE, "用例参数");
                    luckyclient.publicclass.LogUtil.APP.info("用例：" + testcaseob.getSign() + "解析包名：" + packagename + " 方法名：" + functionname + " 第" + (j + 1) + "个参数：" + parameterValues);
                    caselog.caseLogDetail(taskid, testcaseob.getSign(), "解析包名：" + packagename + " 方法名：" + functionname + " 第" + (j + 1) + "个参数：" + parameterValues, "info", String.valueOf(i + 1), "");
                    getParameterValues[j] = parameterValues;
                }
            } else {
                getParameterValues = null;
            }
            // 调用动态方法，执行测试用例
            try {
                luckyclient.publicclass.LogUtil.APP.info("开始调用方法：" + functionname + " .....");
                caselog.caseLogDetail(taskid, testCaseExternalId, "开始调用方法：" + functionname + " .....", "info", String.valueOf(i + 1), "");
                testnote = InvokeMethod.callCase(packagename, functionname, getParameterValues, steps.get(i).getSteptype(), steps.get(i).getExtend());
                testnote = ActionManageForSteps.actionManage(casescript.get("Action"), testnote);
                // 判断结果
                int stepresult = interfaceJudgeResult(testcaseob, steps.get(i), taskid, expectedresults, testnote, caselog);
    			// 失败，并且不在继续,直接终止
                if (0 != stepresult) {
                	setcaseresult = stepresult;
                    if (testcaseob.getFailcontinue() == 0) {
                        luckyclient.publicclass.LogUtil.APP.error("用例【"+testcaseob.getSign()+"】第【"+steps.get(i).getStepnum()+"】步骤执行失败，中断本条用例后续步骤执行，进入到下一条用例执行中......");
                        break;
                    } else {
                        luckyclient.publicclass.LogUtil.APP.error("用例【"+testcaseob.getSign()+"】第【"+steps.get(i).getStepnum()+"】步骤执行失败，继续本条用例后续步骤执行，进入下个步骤执行中......");
                    }
                }
                
            } catch (Exception e) {
                luckyclient.publicclass.LogUtil.ERROR.error("调用方法过程出错，方法名：" + functionname + " 请重新检查脚本方法名称以及参数！");
                caselog.caseLogDetail(taskid, testCaseExternalId, "调用方法过程出错，方法名：" + functionname + " 请重新检查脚本方法名称以及参数！", "error", String.valueOf(i + 1), "");
                luckyclient.publicclass.LogUtil.ERROR.error(e, e);
                testnote = "CallCase调用出错！";
                setcaseresult = 1;
                e.printStackTrace();
                if (testcaseob.getFailcontinue() == 0) {
                    luckyclient.publicclass.LogUtil.APP.error("用例【"+testcaseob.getSign()+"】第【"+(i + 1)+"】步骤执行失败，中断本条用例后续步骤执行，进入到下一条用例执行中......");
                    break;
                } else {
                    luckyclient.publicclass.LogUtil.APP.error("用例【"+testcaseob.getSign()+"】第【"+(i + 1)+"】步骤执行失败，继续本条用例后续步骤执行，进入下个步骤执行中......");
                }
            }
        }
        
        VARIABLE.clear(); // 清空传参MAP
        // 如果调用方法过程中未出错，进入设置测试结果流程
        if (!testnote.contains("CallCase调用出错！") && !testnote.contains("解析出错啦！")) {
            luckyclient.publicclass.LogUtil.APP.info("用例 " + testCaseExternalId + "解析成功，并成功调用用例中方法，请继续查看执行结果！");
            caselog.caseLogDetail(taskid, testCaseExternalId, "解析成功，并成功调用用例中方法，请继续查看执行结果！", "info", "SETCASERESULT...", "");
            caselog.updateCaseDetail(taskid, testCaseExternalId, setcaseresult);
        } else {
            setcaseresult = 1;
            luckyclient.publicclass.LogUtil.APP.error("用例 " + testCaseExternalId + "解析或是调用步骤中的方法出错！");
            caselog.caseLogDetail(taskid, testCaseExternalId, "解析或是调用步骤中的方法出错！", "error", "SETCASERESULT...", "");
            caselog.updateCaseDetail(taskid, testCaseExternalId, 2);
        }
        if (0 == setcaseresult) {
            luckyclient.publicclass.LogUtil.APP.info("用例 " + testCaseExternalId + "步骤全部执行成功！");
            caselog.caseLogDetail(taskid, testCaseExternalId, "步骤全部执行成功！", "info", "EXECUTECASESUC...", "");
        } else {
            luckyclient.publicclass.LogUtil.APP.error("用例 " + testCaseExternalId + "在执行过程中失败，请检查日志！");
            caselog.caseLogDetail(taskid, testCaseExternalId, "在执行过程中失败，请检查日志！", "error", "EXECUTECASESUC...", "");
        }
        LogOperation.updateTastdetail(taskid, 0);
    }

    /**
     * @param testCaseExternalId 用例编号
     * @param taskid             任务ID
     * @param caselog            日志操作对象
     *                           用于在UI的测试过程中，需要调用接口的测试用例
     * @deprecated
     */
    protected static String oneCaseExecuteForWebDriver(String testCaseExternalId, String taskid, LogOperation caselog) {
        Map<String, String> variable = new HashMap<String, String>(0);
        String packagename = null;
        String functionname = null;
        String expectedresults = null;
        Integer setresult = 1;
        Object[] getParameterValues = null;
        String testnote = "初始化测试结果";
        int k = 0;
        ProjectCase testcaseob = GetServerAPI.cgetCaseBysign(testCaseExternalId);
        List<PublicCaseParams> pcplist = GetServerAPI.cgetParamsByProjectid(String.valueOf(testcaseob.getProjectid()));
        // 把公共参数加入到MAP中
        for (PublicCaseParams pcp : pcplist) {
            variable.put(pcp.getParamsname(), pcp.getParamsvalue());
        }
        List<ProjectCasesteps> steps = GetServerAPI.getStepsbycaseid(testcaseob.getId());
        if (steps.size() == 0) {
            setresult = 2;
            luckyclient.publicclass.LogUtil.APP.error("用例中未找到步骤，请检查！");
            caselog.caseLogDetail(taskid, testcaseob.getSign(), "用例中未找到步骤，请检查！", "error", "1", "");
            testnote = "用例中未找到步骤，请检查！";
        }
        // 进入循环，解析用例所有步骤
        for (int i = 0; i < steps.size(); i++) {
            Map<String, String> casescript = InterfaceAnalyticCase.analyticCaseStep(testcaseob, steps.get(i), taskid, caselog);
            try {
                packagename = casescript.get("PackageName");
                packagename = ChangString.changparams(packagename, variable, "包路径");
                functionname = casescript.get("FunctionName");
                functionname = ChangString.changparams(functionname, variable, "方法名");
            } catch (Exception e) {
                k = 0;
                luckyclient.publicclass.LogUtil.APP.error("用例：" + testcaseob.getSign() + "解析包名或是方法名失败，请检查！");
                caselog.caseLogDetail(taskid, testcaseob.getSign(), "解析包名或是方法名失败，请检查！", "error", String.valueOf(i + 1), "");
                e.printStackTrace();
                break; // 某一步骤失败后，此条用例置为失败退出
            }
            // 用例名称解析出现异常或是单个步骤参数解析异常
            if ((null != functionname && functionname.contains("解析异常")) || k == 1) {
                k = 0;
                testnote = "用例第" + (i + 1) + "步解析出错啦！";
                break;
            }
            expectedresults = casescript.get("ExpectedResults");
            expectedresults = ChangString.changparams(expectedresults, variable, "预期结果");
            // 判断方法是否带参数
            if (casescript.size() > 4) {
                // 获取传入参数，放入对象中
                getParameterValues = new Object[casescript.size() - 4];
                for (int j = 0; j < casescript.size() - 4; j++) {
                    if (casescript.get("FunctionParams" + (j + 1)) == null) {
                        k = 1;
                        break;
                    }
                    String parameterValues = casescript.get("FunctionParams" + (j + 1));
                    parameterValues = ChangString.changparams(parameterValues, variable, "用例参数");
                    luckyclient.publicclass.LogUtil.APP.info("用例：" + testcaseob.getSign() + "解析包名：" + packagename + " 方法名：" + functionname + " 第" + (j + 1) + "个参数：" + parameterValues);
                    caselog.caseLogDetail(taskid, testcaseob.getSign(), "解析包名：" + packagename + " 方法名：" + functionname + " 第" + (j + 1) + "个参数：" + parameterValues, "info", String.valueOf(i + 1), "");
                    getParameterValues[j] = parameterValues;
                }
            } else {
                getParameterValues = null;
            }
            // 调用动态方法，执行测试用例
            try {
                luckyclient.publicclass.LogUtil.APP.info("开始调用方法：" + functionname + " .....");

                testnote = InvokeMethod.callCase(packagename, functionname, getParameterValues, steps.get(i).getSteptype(), steps.get(i).getExtend());
                testnote = ActionManageForSteps.actionManage(casescript.get("Action"), testnote);
                if (null != expectedresults && !expectedresults.isEmpty()) {
                    luckyclient.publicclass.LogUtil.APP.info("expectedResults=【" + expectedresults + "】");
                    // 赋值传参
                    if (expectedresults.length() > ASSIGNMENT_SIGN.length() && expectedresults.startsWith(ASSIGNMENT_SIGN)) {
                        variable.put(expectedresults.substring(ASSIGNMENT_SIGN.length()), testnote);
                        luckyclient.publicclass.LogUtil.APP.info("用例：" + testcaseob.getSign() + " 第" + (i + 1) + "步，将测试结果【" + testnote + "】赋值给变量【" + expectedresults.substring(ASSIGNMENT_SIGN.length()) + "】");
                    }
                    // 模糊匹配
                    else if (expectedresults.length() > FUZZY_MATCHING_SIGN.length() && expectedresults.startsWith(FUZZY_MATCHING_SIGN)) {
                        if (testnote.contains(expectedresults.substring(FUZZY_MATCHING_SIGN.length()))) {
                            setresult = 0;
                            luckyclient.publicclass.LogUtil.APP.info("用例：" + testcaseob.getSign() + " 第" + (i + 1) + "步，模糊匹配预期结果成功！执行结果：" + testnote);
                        } else {
                            setresult = 1;
                            luckyclient.publicclass.LogUtil.APP.error("用例：" + testcaseob.getSign() + " 第" + (i + 1) + "步，模糊匹配预期结果失败！预期结果：" + expectedresults.substring(FUZZY_MATCHING_SIGN.length()) + "，测试结果：" + testnote);
                            testnote = "用例第" + (i + 1) + "步，模糊匹配预期结果失败！";
                            break; // 某一步骤失败后，此条用例置为失败退出
                        }
                    }
                    // 正则匹配
                    else if (expectedresults.length() > REGULAR_MATCHING_SIGN.length() && expectedresults.startsWith(REGULAR_MATCHING_SIGN)) {
                        Pattern pattern = Pattern.compile(expectedresults.substring(REGULAR_MATCHING_SIGN.length()));
                        Matcher matcher = pattern.matcher(testnote);
                        if (matcher.find()) {
                            setresult = 0;
                            luckyclient.publicclass.LogUtil.APP.info("用例：" + testcaseob.getSign() + " 第" + (i + 1) + "步，正则匹配预期结果成功！执行结果：" + testnote);
                        } else {
                            setresult = 1;
                            luckyclient.publicclass.LogUtil.APP.error("用例：" + testcaseob.getSign() + " 第" + (i + 1) + "步，正则匹配预期结果失败！预期结果：" + expectedresults.substring(REGULAR_MATCHING_SIGN.length()) + "，测试结果：" + testnote);
                            testnote = "用例第" + (i + 1) + "步，正则匹配预期结果失败！";
                            break; // 某一步骤失败后，此条用例置为失败退出
                        }
                    }
                    // 完全相等
                    else {
                        if (expectedresults.equals(testnote)) {
                            setresult = 0;
                            luckyclient.publicclass.LogUtil.APP.info("用例：" + testcaseob.getSign() + " 第" + (i + 1) + "步，精确匹配预期结果成功！执行结果：" + testnote);
                        } else {
                            setresult = 1;
                            luckyclient.publicclass.LogUtil.APP.error("用例：" + testcaseob.getSign() + " 第" + (i + 1) + "步，精确匹配预期结果失败！预期结果：" + expectedresults + "，测试结果：" + testnote);
                            testnote = "用例第" + (i + 1) + "步，精确匹配预期结果失败！";
                            break; // 某一步骤失败后，此条用例置为失败退出
                        }
                    }
                }
            } catch (Exception e) {
                LogUtil.ERROR.error("调用方法过程出错，方法名：" + functionname + " 请重新检查脚本方法名称以及参数！");
                LogUtil.ERROR.error(e, e);
                testnote = "CallCase调用出错！";
                setresult = 1;
                e.printStackTrace();
                break;
            }
        }
        variable.clear(); // 清空传参MAP
        if (0 == setresult) {
            luckyclient.publicclass.LogUtil.APP.info("用例 " + testcaseob.getSign() + "步骤全部执行成功！");
        } else {
            luckyclient.publicclass.LogUtil.APP.error("用例 " + testcaseob.getSign() + "在执行过程中失败，请检查日志！");
        }
        return testnote;
    }

    /**
     * 
     * @param testCaseExternalId
     * @param taskid
     * @param caselog
     * @param driver
     * @return
     * @throws InterruptedException
     * 提供给Web用例中，runcase的时候使用
     */
    protected static String oneCaseExecuteForUICase(String testCaseExternalId, String taskid, LogOperation caselog, Object driver) throws InterruptedException {
        String expectedresults = null;
        Integer setresult = 1;
        String testnote = "初始化测试结果";
        ProjectCase testcaseob = GetServerAPI.cgetCaseBysign(testCaseExternalId);
        List<PublicCaseParams> pcplist = GetServerAPI.cgetParamsByProjectid(String.valueOf(testcaseob.getProjectid()));
        // 把公共参数加入到MAP中
        for (PublicCaseParams pcp : pcplist) {
        	VARIABLE.put(pcp.getParamsname(), pcp.getParamsvalue());
        }
        List<ProjectCasesteps> steps = GetServerAPI.getStepsbycaseid(testcaseob.getId());
        if (steps.size() == 0) {
            setresult = 2;
            luckyclient.publicclass.LogUtil.APP.error("用例中未找到步骤，请检查！");
            caselog.caseLogDetail(taskid, testcaseob.getSign(), "用例中未找到步骤，请检查！", "error", "1", "");
            testnote = "用例中未找到步骤，请检查！";
        }

        // 进入循环，解析用例所有步骤
        for (ProjectCasesteps step : steps) {
            Map<String, String> params;
            String result;

            // 根据步骤类型来分析步骤参数
            if (1 == step.getSteptype()){
            	params = WebDriverAnalyticCase.analyticCaseStep(testcaseob, step, taskid, caselog);
            }else if (4 == step.getSteptype()){
            	params = AppDriverAnalyticCase.analyticCaseStep(testcaseob, step, taskid,caselog);
            } else{
            	params = InterfaceAnalyticCase.analyticCaseStep(testcaseob, step, taskid, caselog);
            } 

            // 判断分析步骤参数是否有异常
            if (params.get("exception") != null && params.get("exception").contains("解析异常")) {
                setresult = 2;
                break;
            }

            expectedresults = params.get("ExpectedResults");
            expectedresults = ChangString.changparams(expectedresults, VARIABLE, "预期结果");
            
            // 根据步骤类型来执行步骤
            if (1 == step.getSteptype()){
            	WebDriver wd=(WebDriver)driver;
            	result = WebCaseExecution.runWebStep(params, VARIABLE, wd, taskid, testcaseob.getSign(), step.getStepnum(), caselog);
                // 判断结果
                setresult = WebCaseExecution.judgeResult(testcaseob, step, params, wd, taskid, expectedresults, result, caselog);
            }else if (4 == step.getSteptype()){
            	if (driver instanceof AndroidDriver){
            		AndroidDriver<AndroidElement> ad=(AndroidDriver<AndroidElement>)driver;
            		result = AndroidCaseExecution.androidRunStep(params, VARIABLE, ad, taskid, testcaseob.getSign(), step.getStepnum(), caselog);
            		// 判断结果
                    setresult = AndroidCaseExecution.judgeResult(testcaseob, step, params, ad, taskid, expectedresults, result, caselog);
            	}else{
            		IOSDriver<IOSElement> ios=(IOSDriver<IOSElement>)driver;
            		result = IosCaseExecution.iosRunStep(params, VARIABLE, ios, taskid, testcaseob.getSign(), step.getStepnum(), caselog);
            		// 判断结果
                    setresult = IosCaseExecution.judgeResult(testcaseob, step, params, ios, taskid, expectedresults, result, caselog);
            	}
            	
            } else{
            	result = runStep(params, VARIABLE, taskid, testcaseob.getSign(), step, caselog);
            	// 判断结果
            	setresult = interfaceJudgeResult(testcaseob, step, taskid, expectedresults, testnote, caselog);
            } 

            if (0 != setresult){
            	break;
            }
        }

        VARIABLE.clear(); // 清空传参MAP
        if (0 == setresult) {
            testnote = "调用用例【" + testcaseob.getSign() + "】执行成功！";
            luckyclient.publicclass.LogUtil.APP.info("用例 " + testcaseob.getSign() + "步骤全部执行成功！");
        } else {
            testnote = "调用用例【" + testcaseob.getSign() + "】执行失败，请检查日志！";
            luckyclient.publicclass.LogUtil.APP.error("用例 " + testcaseob.getSign() + "在执行过程中失败，请检查日志！");
        }
        return testnote;
    }
    
    /**
     * 其他类型测试用例中调用接口测试步骤
     * @param params
     * @param variable
     * @param taskid
     * @param casenum
     * @param step
     * @param caselog
     * @return
     */
    public static String runStep(Map<String, String> params, Map<String, String> variable, String taskid, String casenum, ProjectCasesteps step, LogOperation caselog) {
        String result = "";
        String packagename = "";
        String functionname = "";
        Object[] getParameterValues = null;

        try {
            packagename = params.get("PackageName");
            packagename = ChangString.changparams(packagename, variable, "包路径");
            functionname = params.get("FunctionName");
            functionname = ChangString.changparams(functionname, variable, "方法名");

            if (null != functionname && functionname.contains("解析异常")) {
                LogUtil.APP.error("用例: " + casenum + ", 解析这个方法【" + functionname + "】失败！");
                caselog.caseLogDetail(taskid, casenum, "用例: " + casenum + ", 解析这个方法【" + functionname + "】失败！", "error", String.valueOf(step.getStepnum()), "");
                result = "步骤执行失败：解析用例失败!";
            } else {
                // 判断方法是否带参数
                if (params.size() > 4) {
                    // 获取传入参数，放入对象中
                    getParameterValues = new Object[params.size() - 4];
                    for (int j = 0; j < params.size() - 4; j++) {
                        if (params.get("FunctionParams" + (j + 1)) == null) {
                            break;
                        }
                        String parameterValues = params.get("FunctionParams" + (j + 1));
                        parameterValues = ChangString.changparams(parameterValues, variable, "用例参数");
                        luckyclient.publicclass.LogUtil.APP.info("用例: " + casenum + ", 解析包路径：" + packagename + "; 方法名：" + functionname + " 第" + (j + 1) + "个参数：" + parameterValues);
                        caselog.caseLogDetail(taskid, casenum, "用例: " + casenum + ", 解析包名：" + packagename + " 方法名：" + functionname + " 第" + (j + 1) + "个参数：" + parameterValues, "info", String.valueOf(step.getStepnum()), "");
                        getParameterValues[j] = parameterValues;
                    }
                } else {
                    getParameterValues = null;
                }

                LogUtil.APP.info("二次解析用例过程完成，等待进行接口操作......");
                caselog.caseLogDetail(taskid, casenum, "包路径: " + packagename + "; 方法名: " + functionname, "info", String.valueOf(step.getStepnum()), "");

                result = InvokeMethod.callCase(packagename, functionname, getParameterValues, step.getSteptype(), step.getExtend());
                result = ActionManageForSteps.actionManage(step.getAction(), result);
            }
        } catch (Exception e) {
            LogUtil.APP.error("调用方法过程出错，方法名：" + functionname + "，请重新检查脚本方法名称以及参数！");
            result = "步骤执行失败：接口调用出错！";
        }
        if (result.contains("步骤执行失败：")) caselog.caseLogDetail(taskid, casenum, result, "error", String.valueOf(step.getStepnum()), "");
        else caselog.caseLogDetail(taskid, casenum, result, "info", String.valueOf(step.getStepnum()), "");
        return result;
    }
    
    private static int interfaceJudgeResult(ProjectCase testcase, ProjectCasesteps step, String taskid, String expectedresults, String testnote, LogOperation caselog){
        int setresult = 0;
        try{
        	if (null != expectedresults && !expectedresults.isEmpty()) {
                luckyclient.publicclass.LogUtil.APP.info("expectedResults=【" + expectedresults + "】");
                // 赋值传参
                if (expectedresults.length() > ASSIGNMENT_SIGN.length() && expectedresults.startsWith(ASSIGNMENT_SIGN)) {
                	VARIABLE.put(expectedresults.substring(ASSIGNMENT_SIGN.length()), testnote);
                    luckyclient.publicclass.LogUtil.APP.info("用例：" + testcase.getSign() + " 第" + step.getStepnum() + "步，将测试结果【" + testnote + "】赋值给变量【" + expectedresults.substring(ASSIGNMENT_SIGN.length()) + "】");
                    caselog.caseLogDetail(taskid, testcase.getSign(), "将测试结果【" + testnote + "】赋值给变量【" + expectedresults.substring(ASSIGNMENT_SIGN.length()) + "】", "info", String.valueOf(step.getStepnum()), "");
                }
                // 模糊匹配
                else if (expectedresults.length() > FUZZY_MATCHING_SIGN.length() && expectedresults.startsWith(FUZZY_MATCHING_SIGN)) {
                    if (testnote.contains(expectedresults.substring(FUZZY_MATCHING_SIGN.length()))) {
                        luckyclient.publicclass.LogUtil.APP.info("用例：" + testcase.getSign() + " 第" + step.getStepnum() + "步，模糊匹配预期结果成功！执行结果：" + testnote);
                        caselog.caseLogDetail(taskid, testcase.getSign(), "模糊匹配预期结果成功！执行结果：" + testnote, "info", String.valueOf(step.getStepnum()), "");
                    } else {
                    	setresult = 1;
                        luckyclient.publicclass.LogUtil.APP.error("用例：" + testcase.getSign() + " 第" + step.getStepnum() + "步，模糊匹配预期结果失败！预期结果：" + expectedresults.substring(FUZZY_MATCHING_SIGN.length()) + "，测试结果：" + testnote);
                        caselog.caseLogDetail(taskid, testcase.getSign(), "模糊匹配预期结果失败！预期结果：" + expectedresults.substring(FUZZY_MATCHING_SIGN.length()) + "，测试结果：" + testnote, "error", String.valueOf(step.getStepnum()), "");
                        testnote = "用例第" + step.getStepnum() + "步，模糊匹配预期结果失败！";
                    }
                }
                // 正则匹配
                else if (expectedresults.length() > REGULAR_MATCHING_SIGN.length() && expectedresults.startsWith(REGULAR_MATCHING_SIGN)) {
                    Pattern pattern = Pattern.compile(expectedresults.substring(REGULAR_MATCHING_SIGN.length()));
                    Matcher matcher = pattern.matcher(testnote);
                    if (matcher.find()) {
                        luckyclient.publicclass.LogUtil.APP.info("用例：" + testcase.getSign() + " 第" + step.getStepnum() + "步，正则匹配预期结果成功！执行结果：" + testnote);
                        caselog.caseLogDetail(taskid, testcase.getSign(), "正则匹配预期结果成功！执行结果：" + testnote, "info", String.valueOf(step.getStepnum()), "");
                    } else {
                        setresult = 1;
                        luckyclient.publicclass.LogUtil.APP.error("用例：" + testcase.getSign() + " 第" + step.getStepnum() + "步，正则匹配预期结果失败！预期结果：" + expectedresults.substring(REGULAR_MATCHING_SIGN.length()) + "，测试结果：" + testnote);
                        caselog.caseLogDetail(taskid, testcase.getSign(), "正则匹配预期结果失败！预期结果：" + expectedresults.substring(REGULAR_MATCHING_SIGN.length()) + "，测试结果：" + testnote, "error", String.valueOf(step.getStepnum()), "");
                        testnote = "用例第" + step.getStepnum() + "步，正则匹配预期结果失败！";
                    }
                }
                // 完全相等
                else {
                    if (expectedresults.equals(testnote)) {
                        luckyclient.publicclass.LogUtil.APP.info("用例：" + testcase.getSign() + " 第" + step.getStepnum() + "步，精确匹配预期结果成功！执行结果：" + testnote);
                        caselog.caseLogDetail(taskid, testcase.getSign(), "精确匹配预期结果成功！执行结果：" + testnote, "info", String.valueOf(step.getStepnum()), "");
                    } else {
                        setresult = 1;
                        luckyclient.publicclass.LogUtil.APP.error("用例：" + testcase.getSign() + " 第" + step.getStepnum() + "步，精确匹配预期结果失败！预期结果：" + expectedresults + "，测试结果：" + testnote);
                        caselog.caseLogDetail(taskid, testcase.getSign(), "精确匹配预期结果失败！预期结果：" + expectedresults + "，测试结果：" + testnote, "error", String.valueOf(step.getStepnum()), "");
                        testnote = "用例第" + step.getStepnum() + "步，精确匹配预期结果失败！";
                    }
                }
            }
        }catch(Exception e){
        	e.printStackTrace();
        	setresult = 2; 
        	return setresult;
        }
        return setresult; 	
    }
    
    
}
