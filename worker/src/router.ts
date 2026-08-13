import { Env } from './types';

type Handler = (
  req: Request,
  env: Env,
  ctx: ExecutionContext,
  params: Record<string, string>
) => Promise<Response>;

interface Route {
  method: string;
  pattern: RegExp;
  paramNames: string[];
  handler: Handler;
}

export class Router {
  private routes: Route[] = [];

  get(path: string, handler: Handler): void {
    this.add('GET', path, handler);
  }

  post(path: string, handler: Handler): void {
    this.add('POST', path, handler);
  }

  put(path: string, handler: Handler): void {
    this.add('PUT', path, handler);
  }

  delete(path: string, handler: Handler): void {
    this.add('DELETE', path, handler);
  }

  all(path: string, handler: Handler): void {
    for (const m of ['GET', 'POST', 'PUT', 'DELETE']) {
      this.add(m, path, handler);
    }
  }

  private add(method: string, path: string, handler: Handler): void {
    const paramNames: string[] = [];
    const patternStr = path.replace(/:([a-zA-Z0-9_]+)/g, (_, name) => {
      paramNames.push(name);
      return '([^/]+)';
    });
    this.routes.push({
      method,
      pattern: new RegExp(`^${patternStr}$`),
      paramNames,
      handler,
    });
  }

  async serve(
    method: string,
    pathname: string,
    req: Request,
    env: Env,
    ctx: ExecutionContext
  ): Promise<Response | null> {
    for (const route of this.routes) {
      if (route.method !== method) continue;
      const match = route.pattern.exec(pathname);
      if (!match) continue;
      const params: Record<string, string> = {};
      route.paramNames.forEach((name, i) => {
        params[name] = decodeURIComponent(match[i + 1]);
      });
      return await route.handler(req, env, ctx, params);
    }
    return null;
  }
}