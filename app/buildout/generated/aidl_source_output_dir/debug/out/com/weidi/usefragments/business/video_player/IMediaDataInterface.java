/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package com.weidi.usefragments.business.video_player;
// Declare any non-default types here with import statements

public interface IMediaDataInterface extends android.os.IInterface
{
  /** Default implementation for IMediaDataInterface. */
  public static class Default implements com.weidi.usefragments.business.video_player.IMediaDataInterface
  {
    /**
         * Demonstrates some basic types that you can use as parameters
         * and return values in AIDL.
         *//*void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
                double aDouble, String aString);*/
    @Override public boolean isAudioFull() throws android.os.RemoteException
    {
      return false;
    }
    @Override public boolean isVideoFull() throws android.os.RemoteException
    {
      return false;
    }
    @Override public void setAudioFull(boolean isFull) throws android.os.RemoteException
    {
    }
    @Override public void setVideoFull(boolean isFull) throws android.os.RemoteException
    {
    }
    @Override public void addAudioData(com.weidi.usefragments.business.video_player.AVPacket data) throws android.os.RemoteException
    {
    }
    @Override public void addVideoData(com.weidi.usefragments.business.video_player.AVPacket data) throws android.os.RemoteException
    {
    }
    @Override public com.weidi.usefragments.business.video_player.AVPacket getAudioData(int index) throws android.os.RemoteException
    {
      return null;
    }
    @Override public com.weidi.usefragments.business.video_player.AVPacket getVideoData(int index) throws android.os.RemoteException
    {
      return null;
    }
    @Override public int audioSize() throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int videoSize() throws android.os.RemoteException
    {
      return 0;
    }
    @Override public void audioClear() throws android.os.RemoteException
    {
    }
    @Override public void videoClear() throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.weidi.usefragments.business.video_player.IMediaDataInterface
  {
    private static final java.lang.String DESCRIPTOR = "com.weidi.usefragments.business.video_player.IMediaDataInterface";
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.weidi.usefragments.business.video_player.IMediaDataInterface interface,
     * generating a proxy if needed.
     */
    public static com.weidi.usefragments.business.video_player.IMediaDataInterface asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.weidi.usefragments.business.video_player.IMediaDataInterface))) {
        return ((com.weidi.usefragments.business.video_player.IMediaDataInterface)iin);
      }
      return new com.weidi.usefragments.business.video_player.IMediaDataInterface.Stub.Proxy(obj);
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
        case TRANSACTION_isAudioFull:
        {
          data.enforceInterface(descriptor);
          boolean _result = this.isAudioFull();
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          return true;
        }
        case TRANSACTION_isVideoFull:
        {
          data.enforceInterface(descriptor);
          boolean _result = this.isVideoFull();
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          return true;
        }
        case TRANSACTION_setAudioFull:
        {
          data.enforceInterface(descriptor);
          boolean _arg0;
          _arg0 = (0!=data.readInt());
          this.setAudioFull(_arg0);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_setVideoFull:
        {
          data.enforceInterface(descriptor);
          boolean _arg0;
          _arg0 = (0!=data.readInt());
          this.setVideoFull(_arg0);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_addAudioData:
        {
          data.enforceInterface(descriptor);
          com.weidi.usefragments.business.video_player.AVPacket _arg0;
          if ((0!=data.readInt())) {
            _arg0 = com.weidi.usefragments.business.video_player.AVPacket.CREATOR.createFromParcel(data);
          }
          else {
            _arg0 = null;
          }
          this.addAudioData(_arg0);
          reply.writeNoException();
          if ((_arg0!=null)) {
            reply.writeInt(1);
            _arg0.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          }
          else {
            reply.writeInt(0);
          }
          return true;
        }
        case TRANSACTION_addVideoData:
        {
          data.enforceInterface(descriptor);
          com.weidi.usefragments.business.video_player.AVPacket _arg0;
          if ((0!=data.readInt())) {
            _arg0 = com.weidi.usefragments.business.video_player.AVPacket.CREATOR.createFromParcel(data);
          }
          else {
            _arg0 = null;
          }
          this.addVideoData(_arg0);
          reply.writeNoException();
          if ((_arg0!=null)) {
            reply.writeInt(1);
            _arg0.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          }
          else {
            reply.writeInt(0);
          }
          return true;
        }
        case TRANSACTION_getAudioData:
        {
          data.enforceInterface(descriptor);
          int _arg0;
          _arg0 = data.readInt();
          com.weidi.usefragments.business.video_player.AVPacket _result = this.getAudioData(_arg0);
          reply.writeNoException();
          if ((_result!=null)) {
            reply.writeInt(1);
            _result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          }
          else {
            reply.writeInt(0);
          }
          return true;
        }
        case TRANSACTION_getVideoData:
        {
          data.enforceInterface(descriptor);
          int _arg0;
          _arg0 = data.readInt();
          com.weidi.usefragments.business.video_player.AVPacket _result = this.getVideoData(_arg0);
          reply.writeNoException();
          if ((_result!=null)) {
            reply.writeInt(1);
            _result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          }
          else {
            reply.writeInt(0);
          }
          return true;
        }
        case TRANSACTION_audioSize:
        {
          data.enforceInterface(descriptor);
          int _result = this.audioSize();
          reply.writeNoException();
          reply.writeInt(_result);
          return true;
        }
        case TRANSACTION_videoSize:
        {
          data.enforceInterface(descriptor);
          int _result = this.videoSize();
          reply.writeNoException();
          reply.writeInt(_result);
          return true;
        }
        case TRANSACTION_audioClear:
        {
          data.enforceInterface(descriptor);
          this.audioClear();
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_videoClear:
        {
          data.enforceInterface(descriptor);
          this.videoClear();
          reply.writeNoException();
          return true;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
    }
    private static class Proxy implements com.weidi.usefragments.business.video_player.IMediaDataInterface
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
      /**
           * Demonstrates some basic types that you can use as parameters
           * and return values in AIDL.
           *//*void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
                  double aDouble, String aString);*/
      @Override public boolean isAudioFull() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isAudioFull, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            return getDefaultImpl().isAudioFull();
          }
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public boolean isVideoFull() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isVideoFull, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            return getDefaultImpl().isVideoFull();
          }
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void setAudioFull(boolean isFull) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(((isFull)?(1):(0)));
          boolean _status = mRemote.transact(Stub.TRANSACTION_setAudioFull, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().setAudioFull(isFull);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setVideoFull(boolean isFull) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(((isFull)?(1):(0)));
          boolean _status = mRemote.transact(Stub.TRANSACTION_setVideoFull, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().setVideoFull(isFull);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void addAudioData(com.weidi.usefragments.business.video_player.AVPacket data) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          if ((data!=null)) {
            _data.writeInt(1);
            data.writeToParcel(_data, 0);
          }
          else {
            _data.writeInt(0);
          }
          boolean _status = mRemote.transact(Stub.TRANSACTION_addAudioData, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().addAudioData(data);
            return;
          }
          _reply.readException();
          if ((0!=_reply.readInt())) {
            data.readFromParcel(_reply);
          }
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void addVideoData(com.weidi.usefragments.business.video_player.AVPacket data) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          if ((data!=null)) {
            _data.writeInt(1);
            data.writeToParcel(_data, 0);
          }
          else {
            _data.writeInt(0);
          }
          boolean _status = mRemote.transact(Stub.TRANSACTION_addVideoData, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().addVideoData(data);
            return;
          }
          _reply.readException();
          if ((0!=_reply.readInt())) {
            data.readFromParcel(_reply);
          }
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public com.weidi.usefragments.business.video_player.AVPacket getAudioData(int index) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        com.weidi.usefragments.business.video_player.AVPacket _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(index);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getAudioData, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            return getDefaultImpl().getAudioData(index);
          }
          _reply.readException();
          if ((0!=_reply.readInt())) {
            _result = com.weidi.usefragments.business.video_player.AVPacket.CREATOR.createFromParcel(_reply);
          }
          else {
            _result = null;
          }
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public com.weidi.usefragments.business.video_player.AVPacket getVideoData(int index) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        com.weidi.usefragments.business.video_player.AVPacket _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(index);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getVideoData, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            return getDefaultImpl().getVideoData(index);
          }
          _reply.readException();
          if ((0!=_reply.readInt())) {
            _result = com.weidi.usefragments.business.video_player.AVPacket.CREATOR.createFromParcel(_reply);
          }
          else {
            _result = null;
          }
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int audioSize() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_audioSize, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            return getDefaultImpl().audioSize();
          }
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int videoSize() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_videoSize, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            return getDefaultImpl().videoSize();
          }
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void audioClear() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_audioClear, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().audioClear();
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void videoClear() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_videoClear, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().videoClear();
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      public static com.weidi.usefragments.business.video_player.IMediaDataInterface sDefaultImpl;
    }
    static final int TRANSACTION_isAudioFull = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_isVideoFull = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_setAudioFull = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_setVideoFull = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_addAudioData = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_addVideoData = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_getAudioData = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_getVideoData = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_audioSize = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_videoSize = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_audioClear = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_videoClear = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    public static boolean setDefaultImpl(com.weidi.usefragments.business.video_player.IMediaDataInterface impl) {
      if (Stub.Proxy.sDefaultImpl == null && impl != null) {
        Stub.Proxy.sDefaultImpl = impl;
        return true;
      }
      return false;
    }
    public static com.weidi.usefragments.business.video_player.IMediaDataInterface getDefaultImpl() {
      return Stub.Proxy.sDefaultImpl;
    }
  }
  /**
       * Demonstrates some basic types that you can use as parameters
       * and return values in AIDL.
       *//*void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
              double aDouble, String aString);*/
  public boolean isAudioFull() throws android.os.RemoteException;
  public boolean isVideoFull() throws android.os.RemoteException;
  public void setAudioFull(boolean isFull) throws android.os.RemoteException;
  public void setVideoFull(boolean isFull) throws android.os.RemoteException;
  public void addAudioData(com.weidi.usefragments.business.video_player.AVPacket data) throws android.os.RemoteException;
  public void addVideoData(com.weidi.usefragments.business.video_player.AVPacket data) throws android.os.RemoteException;
  public com.weidi.usefragments.business.video_player.AVPacket getAudioData(int index) throws android.os.RemoteException;
  public com.weidi.usefragments.business.video_player.AVPacket getVideoData(int index) throws android.os.RemoteException;
  public int audioSize() throws android.os.RemoteException;
  public int videoSize() throws android.os.RemoteException;
  public void audioClear() throws android.os.RemoteException;
  public void videoClear() throws android.os.RemoteException;
}
