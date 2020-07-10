/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package com.weidi.usefragments.business.keeplive;
public interface IGuardAidl extends android.os.IInterface
{
  /** Default implementation for IGuardAidl. */
  public static class Default implements com.weidi.usefragments.business.keeplive.IGuardAidl
  {
    // 相互唤醒服务

    @Override public void wakeUp(java.lang.String title, java.lang.String discription, int iconRes) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.weidi.usefragments.business.keeplive.IGuardAidl
  {
    private static final java.lang.String DESCRIPTOR = "com.weidi.usefragments.business.keeplive.IGuardAidl";
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.weidi.usefragments.business.keeplive.IGuardAidl interface,
     * generating a proxy if needed.
     */
    public static com.weidi.usefragments.business.keeplive.IGuardAidl asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.weidi.usefragments.business.keeplive.IGuardAidl))) {
        return ((com.weidi.usefragments.business.keeplive.IGuardAidl)iin);
      }
      return new com.weidi.usefragments.business.keeplive.IGuardAidl.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      switch (code)
      {
        case INTERFACE_TRANSACTION:
        {
          reply.writeString(descriptor);
          return true;
        }
        case TRANSACTION_wakeUp:
        {
          data.enforceInterface(descriptor);
          java.lang.String _arg0;
          _arg0 = data.readString();
          java.lang.String _arg1;
          _arg1 = data.readString();
          int _arg2;
          _arg2 = data.readInt();
          this.wakeUp(_arg0, _arg1, _arg2);
          reply.writeNoException();
          return true;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
    }
    private static class Proxy implements com.weidi.usefragments.business.keeplive.IGuardAidl
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      // 相互唤醒服务

      @Override public void wakeUp(java.lang.String title, java.lang.String discription, int iconRes) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(title);
          _data.writeString(discription);
          _data.writeInt(iconRes);
          boolean _status = mRemote.transact(Stub.TRANSACTION_wakeUp, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().wakeUp(title, discription, iconRes);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      public static com.weidi.usefragments.business.keeplive.IGuardAidl sDefaultImpl;
    }
    static final int TRANSACTION_wakeUp = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    public static boolean setDefaultImpl(com.weidi.usefragments.business.keeplive.IGuardAidl impl) {
      if (Stub.Proxy.sDefaultImpl == null && impl != null) {
        Stub.Proxy.sDefaultImpl = impl;
        return true;
      }
      return false;
    }
    public static com.weidi.usefragments.business.keeplive.IGuardAidl getDefaultImpl() {
      return Stub.Proxy.sDefaultImpl;
    }
  }
  // 相互唤醒服务

  public void wakeUp(java.lang.String title, java.lang.String discription, int iconRes) throws android.os.RemoteException;
}
