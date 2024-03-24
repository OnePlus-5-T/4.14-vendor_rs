#!/system_android/system/bin/sh

logdump_block='/dev/block/by-name/logdump'

logdump_filesystem=$(/system_android/system/bin/blkid -s TYPE -o value $logdump_block)

if [ "$logdump_filesystem" != "ext4" ]; then
	/system_android/system/bin/dd if=/dev/zero of=$logdump_block bs=1M
	/system_android/system/bin/sync
	/system_android/system/bin/echo "/dev/block/by-name/logdump now empty, it will be formatted at first boot"
else
	/system_android/system/bin/echo "/dev/block/by-name/logdump already formatted in ext4"
fi
