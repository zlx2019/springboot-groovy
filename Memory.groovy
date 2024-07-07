def foo() {
    // 创建一个大小为1MB的数组，每个元素为一个字节
    def array = new byte[1024 * 1024]
    // 这里可以对数组进行一些操作
    println "Array of size ${array.length} bytes has been allocated."
}