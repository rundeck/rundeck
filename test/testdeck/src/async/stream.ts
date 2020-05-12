export async function readStream(str: NodeJS.ReadableStream): Promise<Buffer> {
    return new Promise<Buffer>( (res, rej) => {
        const bufs: Buffer[] = []
        str.on('data', (data: Buffer) => bufs.push(data))
        str.on('end', () => res(Buffer.concat(bufs)))
        str.on('error', rej)
    })
}
