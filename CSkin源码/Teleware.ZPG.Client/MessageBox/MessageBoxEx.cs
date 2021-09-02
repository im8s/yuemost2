using System;
using System.Windows.Forms;
using System.Drawing;
using CCWin.Win32;

namespace Teleware.ZPG.Client
{
    public static class MessageBoxEx
    {
        public static DialogResult Show(
            IWin32Window owner, string text, string caption, MessageBoxButtons buttons,
            Bitmap icon, MessageBoxDefaultButton defaultButton, MessageBoxIcon beepType)
        {
            NativeMethods.MessageBeep((int)beepType);
            MessageBoxForm form = new MessageBoxForm();
            return form.ShowMessageBoxDialog(new MessageBoxArgs(
                owner, text, caption, buttons, icon, defaultButton));
        }

        public static DialogResult Show(
            IWin32Window owner, string text, string caption, MessageBoxButtons buttons,
            MessageBoxIcon icon, MessageBoxDefaultButton defaultButton)
        {
            return Show(owner, text, caption, buttons, GetIcon(icon), defaultButton, icon);
        }

        public static DialogResult Show(
            string text, string caption, MessageBoxButtons buttons,
            MessageBoxIcon icon, MessageBoxDefaultButton defaultButton)
        {
            return Show(null, text, caption, buttons, icon, defaultButton);
        }

        public static DialogResult Show(
            IWin32Window owner, string text, string caption,
            MessageBoxButtons buttons, MessageBoxIcon icon)
        {
            return Show(owner, text, caption, buttons, icon, MessageBoxDefaultButton.Button1);
        }

        public static DialogResult Show(
           string text, string caption,
           MessageBoxButtons buttons, MessageBoxIcon icon)
        {
            return Show(null, text, caption, buttons, icon);
        }

        public static DialogResult Show(
            IWin32Window owner, string text, string caption, MessageBoxButtons buttons)
        {
            return Show(owner, text, caption, buttons, MessageBoxIcon.None);
        }

        public static DialogResult Show(
            string text, string caption, MessageBoxButtons buttons)
        {
            return Show(null, text, caption, buttons);
        }

        public static DialogResult Show(
            IWin32Window owner, string text, string caption)
        {
            return Show(owner, text, caption, MessageBoxButtons.OK);
        }

        public static DialogResult Show(string text, string caption)
        {
            return Show(null, text, caption, MessageBoxButtons.OK);
        }

        public static DialogResult Show(
           IWin32Window owner, string text)
        {
            return Show(owner, text, "");
        }

        public static DialogResult Show(string text)
        {
            return Show((IWin32Window)null, text);
        }

        private static Bitmap GetIcon(MessageBoxIcon icon)
        {
            switch (icon)
            {
                case MessageBoxIcon.Information:
                    return Properties.Resources.msg_info;
                case MessageBoxIcon.Question:
                    return Properties.Resources.msg_question;
                case MessageBoxIcon.Warning:
                    return Properties.Resources.msg_warning;
                case MessageBoxIcon.Error:
                    return Properties.Resources.msg_error;
                default:
                    return null;
            }
        }
    }
}
