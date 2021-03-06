package luckyclient.mail;

import luckyclient.dblog.LogOperation;
import luckyclient.planapi.entity.TestJobs;

import java.util.Properties;

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
public class MailSendInitialization {

    public static void sendMailInitialization(String subject, String content, String taskid, TestJobs testJob, int[] taskCount) {
        boolean isSend = false;
        if (null == taskCount) {
            isSend = true;
        } else {
            if (taskCount.length == 5 && null != testJob) {
                Integer sendCondition = testJob.getSendCondition();
                // 用例全部成功了发送, casecount != casesuc
                if (null!=sendCondition&&1 == sendCondition) {
                    if (taskCount[0] == taskCount[1]) {
                        isSend = true;
                    }
                }
                // 用例部分失败了发送
                if (null!=sendCondition&&2 == sendCondition) {
                    if (taskCount[2] > 0) {
                        isSend = true;
                    }
                }
                // 全发
                if (null!=sendCondition&&0 == sendCondition) {
                    isSend = true;
                }
            }
        }
        if (!isSend) {
            luckyclient.publicclass.LogUtil.APP.info("当前任务不需要发送邮件通知!");
            return;
        }
        String[] addresses = LogOperation.getEmailAddress(taskid);
        Properties properties = luckyclient.publicclass.SysConfig.getConfiguration();
        if (addresses != null) {
            luckyclient.publicclass.LogUtil.APP.info("准备将测试结果发送邮件通知！请稍等。。。。");
            //这个类主要是设置邮件
            MailSenderInfo mailInfo = new MailSenderInfo();
            //这个类主要来发送邮件
            SimpleMailSender sms = new SimpleMailSender();
            mailInfo.setMailServerHost(properties.getProperty("mail.smtp.ip"));
            mailInfo.setMailServerPort(properties.getProperty("mail.smtp.port"));
            mailInfo.setSslenable(properties.getProperty("mail.smtp.ssl.enable").equals("true"));
            mailInfo.setValidate(true);
            mailInfo.setUserName(properties.getProperty("mail.smtp.username"));
            //您的邮箱密码
            mailInfo.setPassword(properties.getProperty("mail.smtp.password"));
            mailInfo.setFromAddress(properties.getProperty("mail.smtp.username"));
            //标题
            mailInfo.setSubject(subject);
            //内容
            mailInfo.setContent(content);
            mailInfo.setToAddresses(addresses);
            //sms.sendHtmlMail(mailInfo);

            StringBuilder stringBuilder = new StringBuilder();
            for (String address : addresses) {
                stringBuilder.append(address).append(";");
            }
            String addressesmail = stringBuilder.toString();
            if (sms.sendHtmlMail(mailInfo)) {
                luckyclient.publicclass.LogUtil.APP.info("给" + addressesmail + "的测试结果通知邮件发送完成！");
            } else {
                luckyclient.publicclass.LogUtil.APP.error("给" + addressesmail + "的测试结果通知邮件发送失败！");
            }
        } else {
            luckyclient.publicclass.LogUtil.APP.info("当前任务不需要发送邮件通知！");
        }
    }

    public static void main(String[] args) {
        // TODO Auto-generated method stub
    }

}
