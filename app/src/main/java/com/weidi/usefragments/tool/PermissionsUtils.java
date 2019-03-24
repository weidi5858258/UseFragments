package com.weidi.usefragments.tool;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;

/***
 首先:
 BaseActivity或当前Activity写上下面代码
 @Override public void onRequestPermissionsResult(int requestCode,
 String[] permissions,
 int[] grantResults) {
 if (permissions != null) {
 for (String permission : permissions) {
 Log.i(TAG, "onRequestPermissionsResult(): " + permission);
 }
 }
 PermissionsUtils.onRequestPermissionsResult(
 this,
 permissions,
 grantResults);
 }

 使用:
 可能需要放在onStart()方法中执行
 PermissionsUtils.checkAndRequestPermission(
 new PermissionsUtils.IRequestPermissionsResult() {

 @Override public Object getRequiredObject() {
 return Main1Fragment.this;
 }

 @Override public String[] getRequiredPermissions() {
 return PermissionsUtils.REQUIRED_PERMISSIONS;
 }

 @Override public void onRequestPermissionsResult(
 String[] permissions,
 int[] grantResults) {
 if (permissions == null || grantResults == null) {
 // 所有权限都同意了
 return;
 }

 PackageManager packageManager = getPackageManager();
 if (PackageManager.PERMISSION_GRANTED == packageManager.checkPermission(
 Manifest.permission.SYSTEM_ALERT_WINDOW,
 mContext.getPackageName())) {
 // do something
 }

 for (String permission : permissions) {
 MLog.i(TAG, "onRequestPermissionsResult() permission: " + permission);
 }

 for (int result : grantResults) {
 MLog.i(TAG, "onRequestPermissionsResult() result: " + result);
 }
 }

 });

 */
public class PermissionsUtils {

    private static final String TAG =
            PermissionsUtils.class.getSimpleName();

    /***
     当一个Activity中需要有多个权限时,
     放在一起申请,然后在回调接口中判断
     有什么样的权限就进行什么样的操作.
     */
    public static final String[] REQUIRED_PERMISSIONS = {
            /*Manifest.permission.VIBRATE,

            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_NUMBERS,*/

            // 存储空间
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            // 相机
            Manifest.permission.CAMERA,
            // 麦克风
            Manifest.permission.RECORD_AUDIO
    };

    public static final String[] REQUIRED_PERMISSIONS_CAMERA = {
            // 相机
            Manifest.permission.CAMERA,
    };

    public static final String[] REQUIRED_PERMISSIONS_RECORD_AUDIO = {
            // 麦克风
            Manifest.permission.RECORD_AUDIO,
    };

    public interface IRequestPermissionsResult {
        Object getRequiredObject();

        String[] getRequiredPermissions();

        /***
         * 同意的权限返回0,不同意的权限返回-1
         *
         * @param permissions
         * @param grantResults
         */
        void onRequestPermissionsResult(
                String[] permissions,
                int[] grantResults);
    }

    /***
     * 其他地方调用这个方法就行了
     * @param requestPermissionsResult
     */
    public static void checkAndRequestPermission(
            IRequestPermissionsResult requestPermissionsResult) {
        RequestPermissions.getInstance().checkAndRequestPermission(
                requestPermissionsResult);
    }

    /***
     系统把结果先返回给BaseActivity,
     然后BaseActivity再返回到这里进行下一步处理
     * @param activity
     * @param permissions
     * @param grantResults
     */
    public static void onRequestPermissionsResult(
            Activity activity,
            String[] permissions,
            int[] grantResults) {
        RequestPermissions.getInstance().onRequestPermissionsResult(
                activity,
                permissions,
                grantResults);
    }

    public static void onRequestPermissionsResult(
            Fragment fragment,
            String[] permissions,
            int[] grantResults) {
        RequestPermissions.getInstance().onRequestPermissionsResult(
                fragment,
                permissions,
                grantResults);
    }

    private static class RequestPermissions {

        private static RequestPermissions sRequestPermissions;
        private IRequestPermissionsResult mIRequestPermissionsResult;

        public static RequestPermissions getInstance() {
            if (sRequestPermissions == null) {
                synchronized (RequestPermissions.class) {
                    if (sRequestPermissions == null) {
                        sRequestPermissions = new RequestPermissions();
                    }
                }
            }
            return sRequestPermissions;
        }

        public void checkAndRequestPermission(
                IRequestPermissionsResult requestPermissionsResult) {
            if (requestPermissionsResult == null
                    || requestPermissionsResult.getRequiredObject() == null) {
                return;
            }

            Object object = requestPermissionsResult.getRequiredObject();
            Activity activity = null;
            Fragment fragment = null;
            if (object instanceof Activity) {
                activity = (Activity) object;
            } else if (object instanceof Fragment) {
                fragment = (Fragment) object;
            }

            if (activity == null && fragment == null) {
                return;
            }

            mIRequestPermissionsResult = requestPermissionsResult;
            String[] permissions = mIRequestPermissionsResult.getRequiredPermissions();
            String[] neededPermissions = null;
            if (activity != null) {
                neededPermissions = getNeedRequestedPermissions(activity, permissions);
            } else {
                neededPermissions = getNeedRequestedPermissions(fragment, permissions);
            }
            if (neededPermissions == null || neededPermissions.length == 0) {
                // 不需要请求权限
                onRequestPermissionsResult(null, null);
                return;
            }

            // 调用系统方法去请求权限
            if (activity != null) {
                activity.requestPermissions(neededPermissions, 0x0001);
            } else {
                fragment.requestPermissions(neededPermissions, 0x0001);
            }
        }

        /***
         系统把结果先返回给BaseActivity,
         然后BaseActivity再返回到这里进行下一步处理

         * @param activity
         * @param permissions 请求时的权限(有序)
         * @param grantResults 请求时的权限结果(有序)
         */
        public void onRequestPermissionsResult(
                Activity activity,
                String[] permissions,
                int[] grantResults) {
            if (activity == null) {
                throw new NullPointerException("onRequestPermissionsResult() activity is null!");
            }

            // 假定所有权限都同意了
            boolean isAllPermissionsGranted = true;
            if (permissions == null ||
                    grantResults == null ||
                    permissions.length == 0 ||
                    grantResults.length == 0) {
                // 这种情况不会发生
                // 没有同意所有的权限请求
                isAllPermissionsGranted = false;
            } else {
                for (int grantResult : grantResults) {
                    // 不相等时表示没有权限
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        isAllPermissionsGranted = false;
                        break;
                    }
                }
            }

            if (isAllPermissionsGranted) {
                onRequestPermissionsResult(null, null);
            } else {
                if (!activity.isDestroyed()) {
                    // 需要请求权限
                    showMissingPermissionDialog(
                            activity, permissions, grantResults);
                }
            }
        }

        public void onRequestPermissionsResult(
                Fragment fragment,
                String[] permissions,
                int[] grantResults) {
            if (fragment == null) {
                throw new NullPointerException("onRequestPermissionsResult() fragment is null!");
            }

            // 假定所有权限都同意了
            boolean isAllPermissionsGranted = true;
            if (permissions == null ||
                    grantResults == null ||
                    permissions.length == 0 ||
                    grantResults.length == 0) {
                // 这种情况不会发生
                // 没有同意所有的权限请求
                isAllPermissionsGranted = false;
            } else {
                for (int grantResult : grantResults) {
                    // 不相等时表示没有权限
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        isAllPermissionsGranted = false;
                        break;
                    }
                }
            }

            if (isAllPermissionsGranted) {
                onRequestPermissionsResult(null, null);
            } else {
                if (!fragment.isDetached()) {
                    // 需要请求权限
                    showMissingPermissionDialog(
                            fragment, permissions, grantResults);
                }
            }
        }

        private static final String dialog_title_help = "Help";
        private static final String dialog_content =
                "This application can\\'t work without required " +
                        "permissions. Please grant the permissions in Settings.";
        private static final String dialog_button_quit = "Quit";
        private static final String dialog_button_settings = "Settings";

        /***
         * 提示用户去设置打开权限
         * @param activity
         */
        private void showMissingPermissionDialog(
                final Activity activity,
                final String[] permissions,
                final int[] grantResults) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(dialog_title_help);
            builder.setMessage(dialog_content);
            // 取消
            builder.setNegativeButton(
                    dialog_button_quit,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (!activity.isDestroyed() && dialog != null) {
                                dialog.dismiss();
                            }

                            onRequestPermissionsResult(permissions, grantResults);
                        }
                    });
            // 设置
            builder.setPositiveButton(
                    dialog_button_settings,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.parse("package:" + activity.getPackageName()));
                            activity.startActivity(intent);
                            // activity.finish();

                            // requires android.permission.GRANT_RUNTIME_PERMISSIONS
                        /*Intent intent = new Intent();
                        ComponentName componentName = new ComponentName(
                                "com.android.packageinstaller",
                                "com.android.packageinstaller.permission.ui
                                .ManagePermissionsActivity");
                        intent.setComponent(componentName);
                        intent.setData(Uri.parse("package:" + activity.getPackageName()));
                        activity.startActivity(intent);
                        activity.finish();*/
                        }
                    });
            builder.show();
        }

        private void showMissingPermissionDialog(
                final Fragment fragment,
                final String[] permissions,
                final int[] grantResults) {
            final Activity activity = fragment.getActivity();
            if (activity == null || activity.isDestroyed()) {
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(dialog_title_help);
            builder.setMessage(dialog_content);
            // 取消
            builder.setNegativeButton(
                    dialog_button_quit,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (!activity.isDestroyed() && dialog != null) {
                                dialog.dismiss();
                            }

                            onRequestPermissionsResult(permissions, grantResults);
                        }
                    });
            // 设置
            builder.setPositiveButton(
                    dialog_button_settings,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.parse("package:" + activity.getPackageName()));
                            activity.startActivity(intent);
                            // activity.finish();

                            // requires android.permission.GRANT_RUNTIME_PERMISSIONS
                        /*Intent intent = new Intent();
                        ComponentName componentName = new ComponentName(
                                "com.android.packageinstaller",
                                "com.android.packageinstaller.permission.ui
                                .ManagePermissionsActivity");
                        intent.setComponent(componentName);
                        intent.setData(Uri.parse("package:" + activity.getPackageName()));
                        activity.startActivity(intent);
                        activity.finish();*/
                        }
                    });
            builder.show();
        }

        private String[] getNeedRequestedPermissions(
                Activity activity,
                String[] permissionNames) {
            if (permissionNames == null || permissionNames.length == 0) {
                return null;
            }

            List<String> needRequestPermission = new ArrayList<String>();
            needRequestPermission.clear();
            for (String permissionName : permissionNames) {
                boolean isPermissionGranted =
                        (PackageManager.PERMISSION_GRANTED ==
                                activity.checkSelfPermission(permissionName));
                if (!isPermissionGranted) {
                    // 被拒绝的权限
                    needRequestPermission.add(permissionName);
                }
            }
            if (needRequestPermission.isEmpty()) {
                return null;
            }

            // 有需要请求的权限
            String[] needRequestPermissionArray = new String[needRequestPermission.size()];
            needRequestPermission.toArray(needRequestPermissionArray);
            return needRequestPermissionArray;
        }

        private String[] getNeedRequestedPermissions(
                Fragment fragment,
                String[] permissionNames) {
            Activity activity = fragment.getActivity();
            if (permissionNames == null
                    || permissionNames.length == 0
                    || activity == null) {
                return null;
            }

            List<String> needRequestPermission = new ArrayList<String>();
            needRequestPermission.clear();
            for (String permissionName : permissionNames) {
                boolean isPermissionGranted =
                        (PackageManager.PERMISSION_GRANTED ==
                                activity.checkSelfPermission(permissionName));
                if (!isPermissionGranted) {
                    // 被拒绝的权限
                    needRequestPermission.add(permissionName);
                }
            }
            if (needRequestPermission.isEmpty()) {
                return null;
            }

            // 有需要请求的权限
            String[] needRequestPermissionArray = new String[needRequestPermission.size()];
            needRequestPermission.toArray(needRequestPermissionArray);
            return needRequestPermissionArray;
        }

        /***
         * permissions为null
         * grantResults为null
         * 时表示所有权限都同意
         *
         * @param permissions
         * @param grantResults
         */
        private void onRequestPermissionsResult(
                String[] permissions,
                int[] grantResults) {
            if (mIRequestPermissionsResult != null) {
                mIRequestPermissionsResult.onRequestPermissionsResult(
                        permissions, grantResults);
            }
        }

    }

}
