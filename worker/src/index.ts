export default {
  async fetch(): Promise<Response> {
    return new Response("placeholder", { status: 200 });
  },
};